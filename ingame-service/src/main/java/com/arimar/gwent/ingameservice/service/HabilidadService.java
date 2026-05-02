package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.*;
import com.arimar.gwent.ingameservice.dto.JugarCartaRequest;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import com.arimar.gwent.ingameservice.entity.CartaPartida;
import com.arimar.gwent.ingameservice.entity.Partida;
import com.arimar.gwent.ingameservice.repository.CartaPartidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabilidadService {

    private final CartaPartidaRepository cartaPartidaRepo;
    private final CartaPartidaStateMachine cartaStateMachine;

    private final Random random = new Random();

    // ==================== ON-PLAY DISPATCH ====================

    /**
     * Processes the ability of a card immediately after it has been placed on the field.
     * Called from PartidaService.jugarCarta() after cartaStateMachine.jugar().
     */
    @Transactional
    public void procesarAlJugar(CartaPartida cartaJugada, Partida partida,
                                 UUID jugadorId, JugarCartaRequest req) {
        log.info("[P-{}] {} jugó '{}' [tipo={}, hab={}, fila={}]",
                partida.getId(), jugadorId,
                cartaJugada.getCartaCatalogo().getNombre(),
                cartaJugada.getCartaCatalogo().getTipo(),
                cartaJugada.getCartaCatalogo().getHabilidad(),
                cartaJugada.getFilaEnCampo());
        switch (cartaJugada.getCartaCatalogo().getHabilidad()) {
            case ESPIA        -> procesarEspia(cartaJugada, partida, jugadorId);
            case MEDICO       -> procesarMedico(cartaJugada, partida, jugadorId,
                                                req.getReviveCartaId(), req.getReviveFila());
            case MUSTER       -> procesarMuster(cartaJugada, partida, jugadorId);
            case DECOY        -> procesarDecoy(cartaJugada, jugadorId, req.getTargetCartaId());
            case CLIMA_LIMPIO -> procesarBuenClima(cartaJugada, partida);
            case SCORCH       -> procesarScorch(cartaJugada, partida, jugadorId, false);
            case SCORCH_FILA  -> procesarScorch(cartaJugada, partida, jugadorId, true);
            case BERSERKER    -> procesarBerserker(cartaJugada, partida, jugadorId);
            case MARDROEME    -> procesarMardroeme(cartaJugada, partida, jugadorId);
            // CUERNO_DEL_COMANDANTE: passive — handled in calcularFila.
            // TORMENTA_SKELLIGE: detected in resolverClimaActivo.
            default           -> { /* NINGUNA, ENLACE, VINCULO, MORAL — passive / LIDER_* handled separately */ }
        }
    }

    // ==================== ACTIVE ABILITIES ====================

    /** ESPIA: moves to opponent's field, player draws 2 cards. */
    private void procesarEspia(CartaPartida carta, Partida partida, UUID jugadorId) {
        UUID oponenteId = esJugadorUno(jugadorId, partida)
                ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        carta.setJugadorId(oponenteId);
        cartaPartidaRepo.save(carta);
        robarCartas(partida, jugadorId, 2);
        log.info("[P-{}] ESPIA: '{}' transferida al oponente {}; jugador {} roba 2 cartas",
                partida.getId(), carta.getCartaCatalogo().getNombre(), oponenteId, jugadorId);
    }

    /** MEDICO: revives a non-hero unit from the graveyard. Skipped if reviveCartaId is null. */
    private void procesarMedico(CartaPartida medico, Partida partida, UUID jugadorId,
                                 Long reviveCartaId, FilaCarta reviveFila) {
        if (reviveCartaId == null) {
            log.info("[P-{}] MEDICO: jugado sin objetivo de revivir", partida.getId());
            return;
        }
        CartaPartida aRevivir = cartaPartidaRepo.findById(reviveCartaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Card to revive not found"));
        validateReviveTarget(aRevivir, jugadorId, medico.getPartida().getId());
        FilaCarta fila = determinarFila(aRevivir.getCartaCatalogo(), reviveFila);
        cartaStateMachine.revivirCarta(aRevivir, fila);
        cartaPartidaRepo.save(aRevivir);
        log.info("[P-{}] MEDICO: revive '{}' en fila {}", partida.getId(),
                aRevivir.getCartaCatalogo().getNombre(), fila);
    }

    /** MUSTER: all copies of this card in the player's deck come into play on the same row. */
    private void procesarMuster(CartaPartida cartaJugada, Partida partida, UUID jugadorId) {
        Long catId = cartaJugada.getCartaCatalogo().getId();
        FilaCarta fila = cartaJugada.getFilaEnCampo();
        List<CartaPartida> copias = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO)
                .stream()
                .filter(cp -> cp.getCartaCatalogo().getId().equals(catId))
                .toList();
        copias.forEach(cp -> cartaStateMachine.jugar(cp, fila));
        cartaPartidaRepo.saveAll(copias);
        log.info("[P-{}] MUSTER: {} copia(s) de '{}' pasan al campo en fila {}",
                partida.getId(), copias.size(), cartaJugada.getCartaCatalogo().getNombre(), fila);
    }

    /** DECOY: chosen non-hero unit on own field returns to hand; DECOY takes its row. */
    private void procesarDecoy(CartaPartida decoy, UUID jugadorId, Long targetCartaId) {
        if (targetCartaId == null)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DECOY requires a target card on your field (targetCartaId)");
        CartaPartida objetivo = cartaPartidaRepo.findById(targetCartaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Target card not found"));
        validateDecoyTarget(objetivo, jugadorId, decoy.getPartida().getId());
        FilaCarta filaObjetivo = objetivo.getFilaEnCampo();
        decoy.setFilaEnCampo(filaObjetivo);
        cartaStateMachine.retornarAMano(objetivo);
        cartaPartidaRepo.save(decoy);
        cartaPartidaRepo.save(objetivo);
        log.info("[P-{}] DECOY: '{}' regresa a la mano; DECOY ocupa fila {}",
                decoy.getPartida().getId(), objetivo.getCartaCatalogo().getNombre(), filaObjetivo);
    }

    /** CLIMA_LIMPIO (Buen clima): removes all active weather cards from field, including itself. */
    private void procesarBuenClima(CartaPartida buenClima, Partida partida) {
        List<CartaPartida> aDescartar = partida.getCartas().stream()
                .filter(cp -> cp.getZona() == ZonaCarta.CAMPO
                        && (cp.getCartaCatalogo().getTipo() == TipoCarta.CLIMA
                                || cp.getId().equals(buenClima.getId())))
                .collect(Collectors.toList());
        aDescartar.forEach(cartaStateMachine::descartar);
        cartaPartidaRepo.saveAll(aDescartar);
        log.info("[P-{}] BUEN CLIMA: {} carta(s) de clima eliminadas del campo",
                buenClima.getPartida().getId(), aDescartar.size() - 1);
    }

    /**
     * SCORCH / SCORCH_FILA:
     * - soloFila=false: destroys strongest unit(s) on BOTH fields if total ≥ 10.
     * - soloFila=true: destroys strongest ENEMY unit(s) in own row if enemy row ≥ 10.
     * Total includes hero strength and Commander's Horn bonus. Heroes CANNOT be destroyed.
     */
    private void procesarScorch(CartaPartida cartaJugada, Partida partida,
                                 UUID jugadorId, boolean soloFila) {
        List<CartaPartida> campoJ1 = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, partida.getJugadorUnoId(), ZonaCarta.CAMPO);
        List<CartaPartida> campoJ2 = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, partida.getJugadorDosId(), ZonaCarta.CAMPO);
        Set<FilaCarta> climaActivo = resolverClimaActivo(campoJ1, campoJ2);

        List<CartaPartida> candidatos;
        if (soloFila) {
            List<CartaPartida> campoOponente = esJugadorUno(jugadorId, partida) ? campoJ2 : campoJ1;
            FilaCarta fila = cartaJugada.getFilaEnCampo();
            List<CartaPartida> filaOp = filtrarFilaUnidades(campoOponente, fila);
            boolean hornOponente = tieneHorn(campoOponente, fila);
            int totalFila = calcularFila(filaOp, climaActivo, fila, hornOponente, false, false, false);
            log.info("[P-{}] SCORCH_FILA en {}: total enemigo={} (horn={}) {}",
                    partida.getId(), fila, totalFila, hornOponente,
                    totalFila >= 10 ? "→ activa" : "→ sin efecto (< 10)");
            if (totalFila < 10) return;
            candidatos = filaOp.stream()
                    .filter(cp -> !cp.getCartaCatalogo().isEsHeroe())
                    .collect(Collectors.toList());
        } else {
            int totalAmbos = calcularFuerzaTotal(campoJ1, climaActivo, HabilidadCarta.NINGUNA, false, false)
                           + calcularFuerzaTotal(campoJ2, climaActivo, HabilidadCarta.NINGUNA, false, false);
            log.info("[P-{}] SCORCH global: total ambos campos={} {}",
                    partida.getId(), totalAmbos,
                    totalAmbos >= 10 ? "→ activa" : "→ sin efecto (< 10)");
            if (totalAmbos < 10) return;
            candidatos = Stream.concat(
                    filtrarTodasUnidades(campoJ1).stream(),
                    filtrarTodasUnidades(campoJ2).stream()
            ).filter(cp -> !cp.getCartaCatalogo().isEsHeroe())
             .collect(Collectors.toList());
        }

        if (candidatos.isEmpty()) return;

        int maxFuerza = candidatos.stream()
                .mapToInt(cp -> fuerzaEfectivaParaScorch(cp, campoJ1, campoJ2, climaActivo))
                .max().orElse(0);
        if (maxFuerza <= 0) return;

        List<CartaPartida> aDestruir = candidatos.stream()
                .filter(cp -> fuerzaEfectivaParaScorch(cp, campoJ1, campoJ2, climaActivo) == maxFuerza)
                .collect(Collectors.toList());
        log.info("[P-{}] SCORCH: destruye {} carta(s) con fuerza efectiva {} → {}",
                partida.getId(), aDestruir.size(), maxFuerza,
                aDestruir.stream().map(cp -> cp.getCartaCatalogo().getNombre()).toList());
        aDestruir.forEach(cartaStateMachine::descartar);
        cartaPartidaRepo.saveAll(aDestruir);

        if (cartaJugada.getCartaCatalogo().getTipo() != TipoCarta.UNIDAD) {
            cartaStateMachine.descartar(cartaJugada);
            cartaPartidaRepo.save(cartaJugada);
        }
    }

    /** MARDROEME: transforms all BERSERKER cards in the same row. */
    private void procesarMardroeme(CartaPartida mardroeme, Partida partida, UUID jugadorId) {
        FilaCarta fila = mardroeme.getFilaEnCampo();
        if (fila == null) return;
        List<CartaPartida> campo = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.CAMPO);
        List<CartaPartida> berserkers = campo.stream()
                .filter(cp -> !cp.isEsSlotLateral()
                        && cp.getFilaEnCampo() == fila
                        && cp.getCartaCatalogo().getHabilidad() == HabilidadCarta.BERSERKER)
                .collect(Collectors.toList());
        berserkers.forEach(cp -> cp.setTransformado(true));
        cartaPartidaRepo.saveAll(berserkers);
        log.info("[P-{}] MARDROEME en fila {}: {} Berserker(s) transformado(s)",
                partida.getId(), fila, berserkers.size());
    }

    /** BERSERKER: transforms immediately if Mardroeme is already present in the same row. */
    private void procesarBerserker(CartaPartida berserker, Partida partida, UUID jugadorId) {
        FilaCarta fila = berserker.getFilaEnCampo();
        if (fila == null) return;
        List<CartaPartida> campo = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.CAMPO);
        boolean hayMardroeme = campo.stream().anyMatch(cp ->
                cp.getFilaEnCampo() == fila
                && cp.getCartaCatalogo().getHabilidad() == HabilidadCarta.MARDROEME);
        if (hayMardroeme) {
            berserker.setTransformado(true);
            cartaPartidaRepo.save(berserker);
            log.info("[P-{}] BERSERKER '{}' transformado en fila {} (fuerza {} → {})",
                    partida.getId(), berserker.getCartaCatalogo().getNombre(), fila,
                    berserker.getCartaCatalogo().getFuerza(),
                    berserker.getCartaCatalogo().getFuerzaTransformada());
        } else {
            log.debug("[P-{}] BERSERKER '{}' en fila {}: sin Mardroeme, no se transforma",
                    partida.getId(), berserker.getCartaCatalogo().getNombre(), fila);
        }
    }

    // ==================== CLIMATE RESOLUTION ====================

    public Set<FilaCarta> resolverClimaActivo(List<CartaPartida> campoJ1, List<CartaPartida> campoJ2) {
        Set<FilaCarta> activo = new HashSet<>();
        Stream.concat(campoJ1.stream(), campoJ2.stream())
                .filter(cp -> cp.getCartaCatalogo().getTipo() == TipoCarta.CLIMA)
                .forEach(cp -> {
                    if (cp.getCartaCatalogo().getHabilidad() == HabilidadCarta.TORMENTA_SKELLIGE) {
                        activo.add(FilaCarta.DISTANCIA);
                        activo.add(FilaCarta.ASEDIO);
                    } else if (cp.getFilaEnCampo() != null) {
                        activo.add(cp.getFilaEnCampo());
                    }
                });
        if (!activo.isEmpty()) log.debug("Clima activo en filas: {}", activo);
        return activo;
    }

    // ==================== PASSIVE MODIFIERS ====================

    /**
     * Calculates total field strength with all passive modifiers applied.
     *
     * @param liderHabilidad  the habilidad of this player's leader card
     * @param liderUsado      whether this player's leader ability has been activated
     * @param liderDobleEspias true if EITHER player has LIDER_DOUBLE_SPIES active (affects both campos)
     */
    public int calcularFuerzaTotal(List<CartaPartida> campo, Set<FilaCarta> climaActivo,
                                    HabilidadCarta liderHabilidad, boolean liderUsado,
                                    boolean liderDobleEspias) {
        HabilidadCarta hab = liderUsado ? liderHabilidad : HabilidadCarta.NINGUNA;
        boolean dobleCac       = (hab == HabilidadCarta.LIDER_DOUBLE_CAC);
        boolean dobleDistancia = (hab == HabilidadCarta.LIDER_DOUBLE_RANGED);
        boolean dobleAsedio    = (hab == HabilidadCarta.LIDER_DOUBLE_SIEGE);
        boolean medioClima     = (hab == HabilidadCarta.LIDER_HALF_WEATHER);

        int cac    = calcularFila(filtrarFilaUnidades(campo, FilaCarta.CUERPO_A_CUERPO), climaActivo, FilaCarta.CUERPO_A_CUERPO,
                            tieneHorn(campo, FilaCarta.CUERPO_A_CUERPO), dobleCac,       medioClima, liderDobleEspias);
        int dist   = calcularFila(filtrarFilaUnidades(campo, FilaCarta.DISTANCIA),        climaActivo, FilaCarta.DISTANCIA,
                            tieneHorn(campo, FilaCarta.DISTANCIA),       dobleDistancia, medioClima, liderDobleEspias);
        int asedio = calcularFila(filtrarFilaUnidades(campo, FilaCarta.ASEDIO),            climaActivo, FilaCarta.ASEDIO,
                            tieneHorn(campo, FilaCarta.ASEDIO),          dobleAsedio,    medioClima, liderDobleEspias);
        log.debug("Fuerza total: CAC={} + DIST={} + ASEDIO={} = {} [liderActivo={} dobleEspias={}]",
                cac, dist, asedio, cac + dist + asedio, hab, liderDobleEspias);
        return cac + dist + asedio;
    }

    /** Public entry for PartidaService to compute per-row strength for TableroDTO. */
    public int calcularFilaPublica(List<CartaPartida> filaUnidades, Set<FilaCarta> climaActivo,
                                    FilaCarta filaActual, boolean tieneHorn,
                                    boolean liderDobleEstaFila, boolean liderMedioClima) {
        return calcularFila(filaUnidades, climaActivo, filaActual, tieneHorn,
                            liderDobleEstaFila, liderMedioClima, false);
    }

    public boolean tieneHorn(List<CartaPartida> campo, FilaCarta fila) {
        return campo.stream().anyMatch(cp ->
                cp.getFilaEnCampo() == fila
                && cp.getCartaCatalogo().getHabilidad() == HabilidadCarta.CUERNO_DEL_COMANDANTE);
    }

    private int calcularFila(List<CartaPartida> filaUnidades, Set<FilaCarta> climaActivo,
                               FilaCarta filaActual, boolean tieneHorn,
                               boolean liderDobleEstaFila, boolean liderMedioClima,
                               boolean liderDobleEspias) {
        boolean hayClima = climaActivo.contains(filaActual);
        boolean debug = log.isDebugEnabled();

        long moralCount = filaUnidades.stream()
                .filter(cp -> cp.getCartaCatalogo().getHabilidad() == HabilidadCarta.REFUERZO_MORAL)
                .count();

        if (debug && !filaUnidades.isEmpty()) {
            log.debug("  [{}] {} unidad(es) | clima={} horn={} liderDoble={} medioClima={} dobleEspias={}",
                    filaActual, filaUnidades.size(), hayClima, tieneHorn,
                    liderDobleEstaFila, liderMedioClima, liderDobleEspias);
        }

        int[] sumas = {0, 0}; // [heroSum, nonHeroSum]
        filaUnidades.forEach(cp -> {
            CartaCatalogo cat = cp.getCartaCatalogo();
            int fuerzaBase = getFuerzaEfectiva(cp);
            if (fuerzaBase == 0) return;
            int fuerza = fuerzaBase;

            // Heroes are immune to all external effects
            if (cat.isEsHeroe()) {
                sumas[0] += fuerza;
                log.debug("    '{}' [HÉROE]: {} (inmune a todo)", cat.getNombre(), fuerza);
                return;
            }

            // Weather: if LIDER_HALF_WEATHER active → ceil(fuerza/2), else → 1
            if (hayClima) {
                int contribucion = liderMedioClima ? (int) Math.ceil(fuerza / 2.0) : 1;
                sumas[1] += contribucion;
                log.debug("    '{}': {} → {} (clima{})",
                        cat.getNombre(), fuerzaBase, contribucion,
                        liderMedioClima ? " HALF_WEATHER ceil(" + fuerzaBase + "/2)" : "");
                return;
            }

            StringBuilder mods = new StringBuilder();

            // LIDER_DOUBLE_SPIES: spy cards doubled before other modifiers
            if (liderDobleEspias && cat.getHabilidad() == HabilidadCarta.ESPIA) {
                fuerza *= 2;
                mods.append(" DOUBLE_SPIES×2");
            }

            // ENLACE_APRETADO / VINCULO_ESTRECHO
            if (cat.getHabilidad() == HabilidadCarta.ENLACE_APRETADO
                    || cat.getHabilidad() == HabilidadCarta.VINCULO_ESTRECHO) {
                long copias = filaUnidades.stream()
                        .filter(c -> c.getCartaCatalogo().getNombre().equals(cat.getNombre()))
                        .count();
                fuerza = (int) (fuerza * copias);
                mods.append(" ENLACE×").append(copias);
            }

            // REFUERZO_MORAL
            long moralSelf = cat.getHabilidad() == HabilidadCarta.REFUERZO_MORAL ? 1 : 0;
            int moralBonus = (int) (moralCount - moralSelf);
            fuerza += moralBonus;
            if (moralBonus > 0) mods.append(" +MORAL+").append(moralBonus);

            sumas[1] += fuerza;
            if (debug) {
                log.debug("    '{}': {} → {}{}",
                        cat.getNombre(), fuerzaBase, fuerza,
                        mods.length() > 0 ? " (" + mods.toString().trim() + ")" : "");
            }
        });

        // Commander's Horn and/or leader doubling: only non-heroes, only when no climate
        int factor = 1;
        if (!hayClima) {
            if (tieneHorn) factor *= 2;
            if (liderDobleEstaFila) factor *= 2;
        }
        int total = sumas[0] + sumas[1] * factor;
        if (debug && !filaUnidades.isEmpty()) {
            log.debug("  [{}] héroes={} no-héroes={} factor={}{}{} → total={}",
                    filaActual, sumas[0], sumas[1], factor,
                    tieneHorn && !hayClima ? " (Horn×2)" : "",
                    liderDobleEstaFila && !hayClima ? " (Líder×2)" : "",
                    total);
        }
        return total;
    }

    /** Returns unit cards in a row, excluding CLIMA and slot-lateral cards. */
    public List<CartaPartida> filtrarFilaUnidades(List<CartaPartida> campo, FilaCarta fila) {
        return campo.stream()
                .filter(cp -> cp.getFilaEnCampo() == fila
                        && cp.getCartaCatalogo().getTipo() != TipoCarta.CLIMA
                        && !cp.isEsSlotLateral())
                .toList();
    }

    // ==================== HELPERS ====================

    private int getFuerzaEfectiva(CartaPartida cp) {
        CartaCatalogo cat = cp.getCartaCatalogo();
        if (cp.isTransformado() && cat.getFuerzaTransformada() != null)
            return cat.getFuerzaTransformada();
        return cat.getFuerza() != null ? cat.getFuerza() : 0;
    }

    /**
     * Computes effective strength of a single card in its row context (for Scorch targeting).
     * Public so LiderService can reuse it.
     */
    public int fuerzaEfectivaParaScorch(CartaPartida cp,
                                         List<CartaPartida> campoJ1, List<CartaPartida> campoJ2,
                                         Set<FilaCarta> climaActivo) {
        CartaCatalogo cat = cp.getCartaCatalogo();
        if (getFuerzaEfectiva(cp) == 0) return 0;

        FilaCarta fila = cp.getFilaEnCampo();
        boolean hayClima = fila != null && climaActivo.contains(fila);
        if (hayClima && !cat.isEsHeroe()) return 1;

        boolean esDeJ1 = cp.getJugadorId().equals(
                campoJ1.isEmpty() ? null : campoJ1.get(0).getJugadorId());
        List<CartaPartida> campoOwner = esDeJ1 ? campoJ1 : campoJ2;
        List<CartaPartida> filaOwner  = filtrarFilaUnidades(campoOwner, fila);

        int fuerza = getFuerzaEfectiva(cp);

        if (cat.getHabilidad() == HabilidadCarta.ENLACE_APRETADO
                || cat.getHabilidad() == HabilidadCarta.VINCULO_ESTRECHO) {
            long copias = filaOwner.stream()
                    .filter(c -> c.getCartaCatalogo().getNombre().equals(cat.getNombre()))
                    .count();
            fuerza = (int) (fuerza * copias);
        }

        if (!cat.isEsHeroe()) {
            long moralCount = filaOwner.stream()
                    .filter(c -> c.getCartaCatalogo().getHabilidad() == HabilidadCarta.REFUERZO_MORAL)
                    .count();
            long moralSelf = cat.getHabilidad() == HabilidadCarta.REFUERZO_MORAL ? 1 : 0;
            fuerza += (moralCount - moralSelf);
        }

        return fuerza;
    }

    private List<CartaPartida> filtrarTodasUnidades(List<CartaPartida> campo) {
        return campo.stream()
                .filter(cp -> !cp.isEsSlotLateral()
                        && cp.getCartaCatalogo().getTipo() != TipoCarta.CLIMA)
                .toList();
    }

    public void robarCartas(Partida partida, UUID jugadorId, int cantidad) {
        List<CartaPartida> mazo = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO);
        Collections.shuffle(mazo, random);
        int aRobar = Math.min(cantidad, mazo.size());
        for (int i = 0; i < aRobar; i++) cartaStateMachine.robar(mazo.get(i));
        if (aRobar > 0) cartaPartidaRepo.saveAll(mazo.subList(0, aRobar));
    }

    private FilaCarta determinarFila(CartaCatalogo carta, FilaCarta filaRequest) {
        if (carta.getFila() == FilaCarta.AGIL) {
            if (filaRequest == null)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Agile cards require a row selection (CUERPO_A_CUERPO or DISTANCIA)");
            if (filaRequest != FilaCarta.CUERPO_A_CUERPO && filaRequest != FilaCarta.DISTANCIA)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Agile cards can only be placed on CUERPO_A_CUERPO or DISTANCIA");
            return filaRequest;
        }
        return carta.getFila();
    }

    private boolean esJugadorUno(UUID jugadorId, Partida partida) {
        return jugadorId.equals(partida.getJugadorUnoId());
    }

    private void validateReviveTarget(CartaPartida carta, UUID jugadorId, Long partidaId) {
        if (!carta.getJugadorId().equals(jugadorId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card does not belong to you");
        if (!carta.getPartida().getId().equals(partidaId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card is not in this match");
        if (carta.getZona() != ZonaCarta.CEMENTERIO)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card is not in your graveyard");
        if (carta.getCartaCatalogo().isEsHeroe())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot revive hero cards");
        if (carta.getCartaCatalogo().getTipo() != TipoCarta.UNIDAD)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Can only revive unit cards");
    }

    private void validateDecoyTarget(CartaPartida carta, UUID jugadorId, Long partidaId) {
        if (!carta.getJugadorId().equals(jugadorId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Target card does not belong to you");
        if (!carta.getPartida().getId().equals(partidaId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Target card is not in this match");
        if (carta.getZona() != ZonaCarta.CAMPO)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Target card is not on the field");
        if (carta.getCartaCatalogo().isEsHeroe())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot target hero cards with DECOY");
    }
}
