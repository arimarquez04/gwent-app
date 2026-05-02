package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.*;
import com.arimar.gwent.ingameservice.dto.UsarLiderRequest;
import com.arimar.gwent.ingameservice.entity.CartaPartida;
import com.arimar.gwent.ingameservice.entity.Mazo;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LiderService {

    private final CartaPartidaRepository cartaPartidaRepo;
    private final CartaPartidaStateMachine cartaStateMachine;
    private final HabilidadService habilidadService;

    private final Random random = new Random();

    /**
     * Dispatches the active leader ability for the given player.
     * Passive abilities (LIDER_DOUBLE_*, LIDER_HALF_WEATHER) only require liderUsado=true
     * to take effect via calcularFuerzaTotal — no additional logic needed here.
     */
    @Transactional
    public void procesarLider(Partida partida, UUID jugadorId, UsarLiderRequest req) {
        HabilidadCarta hab = getLiderHabilidad(partida, jugadorId);
        log.info("[P-{}] {} activa líder: {}", partida.getId(), jugadorId, hab);
        switch (hab) {
            case LIDER_CLEAR_WEATHER      -> procesarClearWeather(partida);
            case LIDER_PICK_FOG           -> procesarPickClima(partida, jugadorId, "Niebla espesa");
            case LIDER_PICK_RAIN          -> procesarPickClima(partida, jugadorId, "Lluvia acida");
            case LIDER_PICK_FROST         -> procesarPickClima(partida, jugadorId, "Tormenta de escarcha");
            case LIDER_STEAL_GRAVEYARD    -> procesarStealGraveyard(partida, jugadorId,
                                                req != null ? req.getTargetCartaPartidaId() : null);
            case LIDER_RESTORE_GRAVEYARD  -> procesarRestoreGraveyard(partida, jugadorId,
                                                req != null ? req.getTargetCartaPartidaId() : null);
            case LIDER_DISCARD_DRAW       -> procesarDiscardDraw(partida, jugadorId,
                                                req != null ? req.getDescartarIds() : null);
            case LIDER_SCORCH_CAC         -> procesarScorchFila(partida, jugadorId, FilaCarta.CUERPO_A_CUERPO);
            case LIDER_SCORCH_RANGED      -> procesarScorchFila(partida, jugadorId, FilaCarta.DISTANCIA);
            case LIDER_REVEAL_HAND        -> procesarRevealHand(partida, jugadorId);
            case LIDER_SHUFFLE_GRAVEYARDS -> procesarShuffleGraveyards(partida);
            // Passive abilities — marking liderUsado=true is sufficient
            case LIDER_DOUBLE_RANGED,
                 LIDER_DOUBLE_SIEGE,
                 LIDER_DOUBLE_CAC,
                 LIDER_DOUBLE_SPIES,
                 LIDER_HALF_WEATHER       -> { /* passive: effect applied in calcularFuerzaTotal */ }
            default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Leader has no active ability");
        }
    }

    // ==================== LEADER ABILITY IMPLEMENTATIONS ====================

    /** LIDER_CLEAR_WEATHER: removes all CLIMA cards from the field (like Buen clima). */
    private void procesarClearWeather(Partida partida) {
        List<CartaPartida> climas = partida.getCartas().stream()
                .filter(cp -> cp.getZona() == ZonaCarta.CAMPO
                        && cp.getCartaCatalogo().getTipo() == TipoCarta.CLIMA)
                .collect(Collectors.toList());
        climas.forEach(cartaStateMachine::descartar);
        cartaPartidaRepo.saveAll(climas);
        log.info("[P-{}] LIDER_CLEAR_WEATHER: {} carta(s) de clima eliminadas", partida.getId(), climas.size());
    }

    /** LIDER_PICK_FOG / LIDER_PICK_RAIN / LIDER_PICK_FROST: plays a named CLIMA card from own deck directly to field. */
    private void procesarPickClima(Partida partida, UUID jugadorId, String nombre) {
        cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO)
                .stream()
                .filter(cp -> cp.getCartaCatalogo().getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .ifPresentOrElse(cp -> {
                    cartaStateMachine.robar(cp);                                      // MAZO → MANO (transitorio)
                    cartaStateMachine.jugar(cp, cp.getCartaCatalogo().getFila());     // MANO → CAMPO
                    cartaPartidaRepo.save(cp);
                    log.info("[P-{}] LIDER_PICK: '{}' sacada del mazo y jugada en fila {}",
                            partida.getId(), nombre, cp.getCartaCatalogo().getFila());
                }, () -> log.info("[P-{}] LIDER_PICK: '{}' no encontrada en el mazo, sin efecto",
                        partida.getId(), nombre));
    }

    /** LIDER_STEAL_GRAVEYARD: take targetId from opponent's CEMENTERIO to own MANO. */
    private void procesarStealGraveyard(Partida partida, UUID jugadorId, Long targetId) {
        if (targetId == null)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "targetCartaPartidaId is required for this leader ability");
        UUID oponenteId = getOponenteId(partida, jugadorId);
        CartaPartida carta = findAndValidateGraveyard(targetId, oponenteId, partida.getId());
        carta.setJugadorId(jugadorId);
        cartaStateMachine.tomarDelCementerio(carta);
        cartaPartidaRepo.save(carta);
        log.info("[P-{}] LIDER_STEAL_GRAVEYARD: '{}' tomada del cementerio del oponente",
                partida.getId(), carta.getCartaCatalogo().getNombre());
    }

    /** LIDER_RESTORE_GRAVEYARD: move targetId from own CEMENTERIO to own MANO. */
    private void procesarRestoreGraveyard(Partida partida, UUID jugadorId, Long targetId) {
        if (targetId == null)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "targetCartaPartidaId is required for this leader ability");
        CartaPartida carta = findAndValidateGraveyard(targetId, jugadorId, partida.getId());
        cartaStateMachine.tomarDelCementerio(carta);
        cartaPartidaRepo.save(carta);
        log.info("[P-{}] LIDER_RESTORE_GRAVEYARD: '{}' recuperada del propio cementerio",
                partida.getId(), carta.getCartaCatalogo().getNombre());
    }

    /** LIDER_DISCARD_DRAW: discard exactly 2 cards from hand, draw 1 from deck. */
    private void procesarDiscardDraw(Partida partida, UUID jugadorId, List<Long> descartarIds) {
        if (descartarIds == null || descartarIds.size() != 2)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "descartarIds must contain exactly 2 card IDs");
        List<CartaPartida> mano = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MANO);
        List<CartaPartida> aDescartar = new ArrayList<>();
        for (Long id : descartarIds) {
            CartaPartida cp = mano.stream().filter(c -> c.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Card " + id + " is not in your hand"));
            aDescartar.add(cp);
        }
        aDescartar.forEach(cartaStateMachine::usarInstantaneo);
        cartaPartidaRepo.saveAll(aDescartar);
        log.info("[P-{}] LIDER_DISCARD_DRAW: descarta {} → {}",
                partida.getId(), aDescartar.size(),
                aDescartar.stream().map(cp -> cp.getCartaCatalogo().getNombre()).toList());

        List<CartaPartida> mazo = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO);
        if (!mazo.isEmpty()) {
            CartaPartida robada = mazo.get(random.nextInt(mazo.size()));
            cartaStateMachine.robar(robada);
            cartaPartidaRepo.save(robada);
            log.info("[P-{}] LIDER_DISCARD_DRAW: roba '{}'", partida.getId(), robada.getCartaCatalogo().getNombre());
        } else {
            log.info("[P-{}] LIDER_DISCARD_DRAW: mazo vacío, no se roba ninguna carta", partida.getId());
        }
    }

    /** LIDER_SCORCH_CAC / LIDER_SCORCH_RANGED: scorch enemy units in a specific row.
     *  Total includes hero strength and Commander's Horn bonus. Heroes are not eligible for destruction. */
    private void procesarScorchFila(Partida partida, UUID jugadorId, FilaCarta filaTarget) {
        UUID oponenteId = getOponenteId(partida, jugadorId);
        List<CartaPartida> campoJ1 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorUnoId(), ZonaCarta.CAMPO);
        List<CartaPartida> campoJ2 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorDosId(), ZonaCarta.CAMPO);
        Set<FilaCarta> climaActivo = habilidadService.resolverClimaActivo(campoJ1, campoJ2);

        List<CartaPartida> campoOponente = oponenteId.equals(partida.getJugadorUnoId()) ? campoJ1 : campoJ2;
        List<CartaPartida> filaOp = habilidadService.filtrarFilaUnidades(campoOponente, filaTarget);

        boolean hornOponente = habilidadService.tieneHorn(campoOponente, filaTarget);
        int total = habilidadService.calcularFilaPublica(filaOp, climaActivo, filaTarget, hornOponente, false, false);
        if (total < 10) return;

        List<CartaPartida> candidatos = filaOp.stream()
                .filter(cp -> !cp.getCartaCatalogo().isEsHeroe())
                .collect(Collectors.toList());
        if (candidatos.isEmpty()) return;

        int maxFuerza = candidatos.stream()
                .mapToInt(cp -> habilidadService.fuerzaEfectivaParaScorch(cp, campoJ1, campoJ2, climaActivo))
                .max().orElse(0);
        List<CartaPartida> aDestruir = candidatos.stream()
                .filter(cp -> habilidadService.fuerzaEfectivaParaScorch(cp, campoJ1, campoJ2, climaActivo) == maxFuerza)
                .collect(Collectors.toList());
        log.info("[P-{}] LIDER SCORCH_{}: total enemigo={} (horn={}) → destruye {} carta(s) con fuerza={}: {}",
                partida.getId(), filaTarget, total, hornOponente, aDestruir.size(), maxFuerza,
                aDestruir.stream().map(cp -> cp.getCartaCatalogo().getNombre()).toList());
        aDestruir.forEach(cp -> {
            cartaStateMachine.descartar(cp);
            cartaPartidaRepo.save(cp);
        });
    }

    /** LIDER_REVEAL_HAND: pick 3 random cards from opponent's hand and store their IDs in Partida. */
    private void procesarRevealHand(Partida partida, UUID jugadorId) {
        UUID oponenteId = getOponenteId(partida, jugadorId);
        List<CartaPartida> manoOponente = cartaPartidaRepo
                .findByPartidaAndJugadorIdAndZona(partida, oponenteId, ZonaCarta.MANO);
        Collections.shuffle(manoOponente, random);
        String ids = manoOponente.stream()
                .limit(3)
                .map(cp -> cp.getId().toString())
                .collect(Collectors.joining(","));
        if (jugadorId.equals(partida.getJugadorUnoId()))
            partida.setCartasReveladasJ1(ids);
        else
            partida.setCartasReveladasJ2(ids);
        log.info("[P-{}] LIDER_REVEAL_HAND: {} carta(s) de la mano del oponente reveladas",
                partida.getId(), manoOponente.stream().limit(3).count());
    }

    /** LIDER_SHUFFLE_GRAVEYARDS: move all CEMENTERIO cards (both players) back to their MAZO. */
    private void procesarShuffleGraveyards(Partida partida) {
        List<CartaPartida> todos = partida.getCartas().stream()
                .filter(cp -> cp.getZona() == ZonaCarta.CEMENTERIO)
                .collect(Collectors.toList());
        todos.forEach(cartaStateMachine::devolverCementerioAlMazo);
        cartaPartidaRepo.saveAll(todos);
        log.info("[P-{}] LIDER_SHUFFLE_GRAVEYARDS: {} carta(s) devueltas a los mazos",
                partida.getId(), todos.size());
    }

    // ==================== HELPERS ====================

    public HabilidadCarta getLiderHabilidad(Partida partida, UUID jugadorId) {
        Mazo mazo = jugadorId.equals(partida.getJugadorUnoId())
                ? partida.getMazoJugadorUno() : partida.getMazoJugadorDos();
        if (mazo.getLider() == null)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Your deck has no leader assigned");
        return mazo.getLider().getHabilidad();
    }

    private UUID getOponenteId(Partida partida, UUID jugadorId) {
        return jugadorId.equals(partida.getJugadorUnoId())
                ? partida.getJugadorDosId() : partida.getJugadorUnoId();
    }

    private CartaPartida findAndValidateGraveyard(Long id, UUID jugadorId, Long partidaId) {
        CartaPartida cp = cartaPartidaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        if (!cp.getJugadorId().equals(jugadorId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card does not belong to the target player");
        if (!cp.getPartida().getId().equals(partidaId))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card is not in this match");
        if (cp.getZona() != ZonaCarta.CEMENTERIO)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card is not in the graveyard");
        return cp;
    }
}
