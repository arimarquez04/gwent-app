package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.client.JugadorServiceClient;
import com.arimar.gwent.ingameservice.domain.enums.*;
import com.arimar.gwent.ingameservice.dto.*;
import com.arimar.gwent.ingameservice.entity.*;
import com.arimar.gwent.ingameservice.repository.CartaPartidaRepository;
import com.arimar.gwent.ingameservice.repository.MazoRepository;
import com.arimar.gwent.ingameservice.repository.PartidaRepository;
import com.arimar.gwent.ingameservice.repository.RondaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartidaService {

    private static final int CARTAS_INICIALES = 10;
    private static final int MAX_MULLIGAN = 2;
    private static final int VIDAS_INICIALES = 2;
    private static final int ROBAR_TRAS_RONDA_1 = 2;
    private static final int ROBAR_TRAS_RONDA_2 = 1;

    private final PartidaRepository partidaRepo;
    private final CartaPartidaRepository cartaPartidaRepo;
    private final RondaRepository rondaRepo;
    private final MazoRepository mazoRepo;
    private final CartaPartidaStateMachine cartaStateMachine;
    private final HabilidadService habilidadService;
    private final LiderService liderService;
    private final JugadorServiceClient jugadorClient;

    private final Random random = new Random();

    // ==================== PUBLIC METHODS ====================

    @Transactional
    public PartidaDTO createPartida(UUID jugadorId, CreatePartidaRequest req) {
        if (jugadorId.equals(req.getOponenteId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot create a match against yourself");
        }

        Mazo mazoUno = mazoRepo.findById(req.getMazoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found: " + req.getMazoId()));
        if (!mazoUno.getJugadorId().equals(jugadorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found: " + req.getMazoId());
        }
        if (mazoUno.getEstado() != EstadoMazo.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Deck is not active: " + req.getMazoId());
        }

        Mazo mazoDos = mazoRepo.findById(req.getMazoOponenteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opponent deck not found: " + req.getMazoOponenteId()));
        if (!mazoDos.getJugadorId().equals(req.getOponenteId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Opponent deck not found: " + req.getMazoOponenteId());
        }
        if (mazoDos.getEstado() != EstadoMazo.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Opponent deck is not active: " + req.getMazoOponenteId());
        }

        Partida partida = new Partida();
        partida.setJugadorUnoId(jugadorId);
        partida.setJugadorDosId(req.getOponenteId());
        partida.setMazoJugadorUno(mazoUno);
        partida.setMazoJugadorDos(mazoDos);
        partida.setEstado(EstadoPartida.MULLIGAN);
        partida.setRondaActual(0);
        partida.setVidasJugadorUno(VIDAS_INICIALES);
        partida.setVidasJugadorDos(VIDAS_INICIALES);
        partida.setTurnoJugadorId(random.nextBoolean() ? jugadorId : req.getOponenteId());

        partida = partidaRepo.save(partida);
        log.info("[P-{}] Partida creada: J1={} (mazo={}) vs J2={} (mazo={})",
                partida.getId(), jugadorId, req.getMazoId(), req.getOponenteId(), req.getMazoOponenteId());

        // LIDER_CANCEL_LEADER (Emhyr: El Blanco Llama) auto-activates at game creation
        HabilidadCarta liderJ1 = getLiderHabilidadSafe(mazoUno);
        HabilidadCarta liderJ2 = getLiderHabilidadSafe(mazoDos);
        if (liderJ1 == HabilidadCarta.LIDER_CANCEL_LEADER || liderJ2 == HabilidadCarta.LIDER_CANCEL_LEADER) {
            partida.setLiderUsadoJugadorUno(true);
            partida.setLiderUsadoJugadorDos(true);
            log.info("[P-{}] LIDER_CANCEL_LEADER: ambos líderes bloqueados permanentemente", partida.getId());
        }

        repartirCartas(partida, jugadorId, mazoUno);
        repartirCartas(partida, req.getOponenteId(), mazoDos);

        return buildPartidaDTO(partida, jugadorId);
    }

    public PartidaDTO getPartida(UUID jugadorId, Long partidaId) {
        Partida partida = findPartida(partidaId);
        validateJugadorEnPartida(jugadorId, partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    @Transactional
    public PartidaDTO mulligan(UUID jugadorId, Long partidaId, MulliganRequest req) {
        Partida partida = findPartida(partidaId);
        validateJugadorEnPartida(jugadorId, partida);

        if (partida.getEstado() != EstadoPartida.MULLIGAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not in mulligan phase");
        }

        boolean esJugadorUno = jugadorId.equals(partida.getJugadorUnoId());
        if ((esJugadorUno && partida.isJugadorUnoMulligan()) || (!esJugadorUno && partida.isJugadorDosMulligan())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mulligan already completed");
        }

        if (req.getCartaPartidaIds().size() > MAX_MULLIGAN) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Can swap at most " + MAX_MULLIGAN + " cards during mulligan");
        }

        List<CartaPartida> mano = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MANO);
        List<CartaPartida> mazo = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO);

        for (Long cpId : req.getCartaPartidaIds()) {
            CartaPartida cartaADevolver = mano.stream()
                    .filter(c -> c.getId().equals(cpId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Card " + cpId + " is not in your hand"));

            cartaStateMachine.devolverAlMazo(cartaADevolver);
            mano.remove(cartaADevolver);
            mazo.add(cartaADevolver);

            if (!mazo.isEmpty()) {
                CartaPartida cartaRobada = mazo.get(random.nextInt(mazo.size()));
                cartaStateMachine.robar(cartaRobada);
                mazo.remove(cartaRobada);
                mano.add(cartaRobada);
            }
        }

        cartaPartidaRepo.saveAll(mano);
        cartaPartidaRepo.saveAll(mazo);

        if (esJugadorUno) {
            partida.setJugadorUnoMulligan(true);
        } else {
            partida.setJugadorDosMulligan(true);
        }

        if (partida.isJugadorUnoMulligan() && partida.isJugadorDosMulligan()) {
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setRondaActual(1);
            log.info("[P-{}] Mulligan completado → EN_CURSO, ronda 1", partida.getId());

            // LIDER_DRAW_EXTRA (Francesca: Flor del Valle) auto-activates when match starts
            if (getLiderHabilidadSafe(partida.getMazoJugadorUno()) == HabilidadCarta.LIDER_DRAW_EXTRA
                    && !partida.isLiderUsadoJugadorUno()) {
                habilidadService.robarCartas(partida, partida.getJugadorUnoId(), 1);
                partida.setLiderUsadoJugadorUno(true);
                log.info("[P-{}] LIDER_DRAW_EXTRA: J1 roba 1 carta extra", partida.getId());
            }
            if (getLiderHabilidadSafe(partida.getMazoJugadorDos()) == HabilidadCarta.LIDER_DRAW_EXTRA
                    && !partida.isLiderUsadoJugadorDos()) {
                habilidadService.robarCartas(partida, partida.getJugadorDosId(), 1);
                partida.setLiderUsadoJugadorDos(true);
                log.info("[P-{}] LIDER_DRAW_EXTRA: J2 roba 1 carta extra", partida.getId());
            }
        } else {
            log.info("[P-{}] {} completa mulligan ({} carta(s) intercambiadas)",
                    partida.getId(), jugadorId, req.getCartaPartidaIds().size());
        }

        partidaRepo.save(partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    @Transactional
    public PartidaDTO jugarCarta(UUID jugadorId, Long partidaId, JugarCartaRequest req) {
        Partida partida = findPartida(partidaId);
        validateJugadorEnPartida(jugadorId, partida);
        validateTurno(jugadorId, partida);

        boolean esJugadorUno = jugadorId.equals(partida.getJugadorUnoId());
        if ((esJugadorUno && partida.isJugadorUnoPaso()) || (!esJugadorUno && partida.isJugadorDosPaso())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already passed this round");
        }

        CartaPartida cartaPartida = cartaPartidaRepo.findById(req.getCartaPartidaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        if (!cartaPartida.getJugadorId().equals(jugadorId) || !cartaPartida.getPartida().getId().equals(partidaId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card does not belong to you in this match");
        }
        if (cartaPartida.getZona() != ZonaCarta.MANO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card is not in your hand");
        }

        CartaCatalogo carta = cartaPartida.getCartaCatalogo();
        TipoCarta tipo = carta.getTipo();
        HabilidadCarta hab = carta.getHabilidad();

        boolean esDecoy         = tipo == TipoCarta.ESPECIAL && hab == HabilidadCarta.DECOY;
        boolean esBuenClima     = tipo == TipoCarta.ESPECIAL && hab == HabilidadCarta.CLIMA_LIMPIO;
        boolean esScorchInst    = tipo == TipoCarta.ESPECIAL && hab == HabilidadCarta.SCORCH;
        boolean esMardroemeSlot = tipo == TipoCarta.ESPECIAL && hab == HabilidadCarta.MARDROEME;
        boolean esHornSlot      = tipo == TipoCarta.ESPECIAL && hab == HabilidadCarta.CUERNO_DEL_COMANDANTE;
        boolean esClima         = tipo == TipoCarta.CLIMA;
        boolean esSlotLateral   = esMardroemeSlot || esHornSlot;

        if (tipo != TipoCarta.UNIDAD && !esDecoy && !esBuenClima && !esScorchInst
                && !esMardroemeSlot && !esHornSlot && !esClima) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only unit, climate, and applicable special cards can be played");
        }

        if (esSlotLateral) {
            FilaCarta filaSlot = req.getFila();
            if (filaSlot == null || filaSlot == FilaCarta.AGIL) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Slot lateral cards require a specific row (CUERPO_A_CUERPO, DISTANCIA or ASEDIO)");
            }
            boolean ocupado = cartaPartidaRepo
                    .findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.CAMPO)
                    .stream()
                    .anyMatch(cp -> cp.isEsSlotLateral() && cp.getFilaEnCampo() == filaSlot);
            if (ocupado) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Row " + filaSlot + " already has a support card (slot lateral occupied)");
            }
        }

        FilaCarta filaDestino = (esDecoy || esBuenClima || esScorchInst) ? null
                : determinarFila(carta, req.getFila());

        if (esSlotLateral) cartaPartida.setEsSlotLateral(true);

        cartaStateMachine.jugar(cartaPartida, filaDestino);
        cartaPartidaRepo.save(cartaPartida);

        habilidadService.procesarAlJugar(cartaPartida, partida, jugadorId, req);

        UUID oponenteId = esJugadorUno ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        boolean oponentePaso = esJugadorUno ? partida.isJugadorDosPaso() : partida.isJugadorUnoPaso();

        List<CartaPartida> manoRestante = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MANO);
        if (manoRestante.isEmpty()) {
            if (esJugadorUno) partida.setJugadorUnoPaso(true);
            else              partida.setJugadorDosPaso(true);
        }

        boolean jugadorPaso = esJugadorUno ? partida.isJugadorUnoPaso() : partida.isJugadorDosPaso();

        if (jugadorPaso && oponentePaso) {
            resolverRonda(partida);
        } else if (!oponentePaso) {
            partida.setTurnoJugadorId(oponenteId);
        }

        partidaRepo.save(partida);
        if (partida.getEstado() == EstadoPartida.TERMINADA) notificarResultado(partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    @Transactional
    public PartidaDTO pasar(UUID jugadorId, Long partidaId) {
        Partida partida = findPartida(partidaId);
        validateJugadorEnPartida(jugadorId, partida);
        validateTurno(jugadorId, partida);

        boolean esJugadorUno = jugadorId.equals(partida.getJugadorUnoId());
        if ((esJugadorUno && partida.isJugadorUnoPaso()) || (!esJugadorUno && partida.isJugadorDosPaso())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already passed this round");
        }

        if (esJugadorUno) partida.setJugadorUnoPaso(true);
        else              partida.setJugadorDosPaso(true);
        log.info("[P-{}] {} pasa en ronda {}", partida.getId(), jugadorId, partida.getRondaActual());

        UUID oponenteId = esJugadorUno ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        boolean oponentePaso = esJugadorUno ? partida.isJugadorDosPaso() : partida.isJugadorUnoPaso();

        if (oponentePaso) {
            resolverRonda(partida);
        } else {
            partida.setTurnoJugadorId(oponenteId);
        }

        partidaRepo.save(partida);
        if (partida.getEstado() == EstadoPartida.TERMINADA) notificarResultado(partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    @Transactional
    public PartidaDTO usarLider(UUID jugadorId, Long partidaId, UsarLiderRequest req) {
        Partida partida = findPartida(partidaId);
        validateJugadorEnPartida(jugadorId, partida);
        validateTurno(jugadorId, partida);

        boolean esJ1 = jugadorId.equals(partida.getJugadorUnoId());
        boolean yaUsado = esJ1 ? partida.isLiderUsadoJugadorUno() : partida.isLiderUsadoJugadorDos();
        if (yaUsado)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leader ability already used");

        if ((esJ1 && partida.isJugadorUnoPaso()) || (!esJ1 && partida.isJugadorDosPaso()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already passed this round");

        liderService.procesarLider(partida, jugadorId, req);

        if (esJ1) partida.setLiderUsadoJugadorUno(true);
        else      partida.setLiderUsadoJugadorDos(true);
        log.info("[P-{}] Líder marcado como usado para {}", partida.getId(), jugadorId);

        // Consume turn
        UUID oponenteId = esJ1 ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        boolean oponentePaso = esJ1 ? partida.isJugadorDosPaso() : partida.isJugadorUnoPaso();

        List<CartaPartida> manoRestante = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MANO);
        if (manoRestante.isEmpty()) {
            if (esJ1) partida.setJugadorUnoPaso(true);
            else      partida.setJugadorDosPaso(true);
        }

        boolean jugadorPaso = esJ1 ? partida.isJugadorUnoPaso() : partida.isJugadorDosPaso();

        if (jugadorPaso && oponentePaso) {
            resolverRonda(partida);
        } else if (!oponentePaso) {
            partida.setTurnoJugadorId(oponenteId);
        }

        partidaRepo.save(partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    // ==================== INTERNAL METHODS ====================

    private void repartirCartas(Partida partida, UUID jugadorId, Mazo mazo) {
        List<CartaPartida> todasLasCartas = new ArrayList<>();

        for (MazoCarta mc : mazo.getCartas()) {
            if (mc.getCartaCatalogo().getTipo() == TipoCarta.LIDER) continue;
            for (int i = 0; i < mc.getCantidad(); i++) {
                CartaPartida cp = new CartaPartida();
                cp.setPartida(partida);
                cp.setJugadorId(jugadorId);
                cp.setCartaCatalogo(mc.getCartaCatalogo());
                cp.setZona(ZonaCarta.MAZO);
                todasLasCartas.add(cp);
            }
        }

        Collections.shuffle(todasLasCartas, random);
        int aDar = Math.min(CARTAS_INICIALES, todasLasCartas.size());
        for (int i = 0; i < aDar; i++) cartaStateMachine.robar(todasLasCartas.get(i));
        cartaPartidaRepo.saveAll(todasLasCartas);
    }

    private void resolverRonda(Partida partida) {
        List<CartaPartida> campoJ1 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorUnoId(), ZonaCarta.CAMPO);
        List<CartaPartida> campoJ2 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorDosId(), ZonaCarta.CAMPO);

        Set<FilaCarta> climaActivo = habilidadService.resolverClimaActivo(campoJ1, campoJ2);

        HabilidadCarta liderHabJ1 = getLiderHabilidadSafe(partida.getMazoJugadorUno());
        boolean liderUsadoJ1 = partida.isLiderUsadoJugadorUno();
        HabilidadCarta liderHabJ2 = getLiderHabilidadSafe(partida.getMazoJugadorDos());
        boolean liderUsadoJ2 = partida.isLiderUsadoJugadorDos();

        boolean dobleEspias = (liderUsadoJ1 && liderHabJ1 == HabilidadCarta.LIDER_DOUBLE_SPIES)
                            || (liderUsadoJ2 && liderHabJ2 == HabilidadCarta.LIDER_DOUBLE_SPIES);

        int fuerzaJ1 = habilidadService.calcularFuerzaTotal(campoJ1, climaActivo, liderHabJ1, liderUsadoJ1, dobleEspias);
        int fuerzaJ2 = habilidadService.calcularFuerzaTotal(campoJ2, climaActivo, liderHabJ2, liderUsadoJ2, dobleEspias);
        log.info("[P-{}] === Ronda {} === J1={} pts vs J2={} pts → {} [liderJ1={} usadoJ1={}, liderJ2={} usadoJ2={}, dobleEspias={}]",
                partida.getId(), partida.getRondaActual(), fuerzaJ1, fuerzaJ2,
                fuerzaJ1 > fuerzaJ2 ? "J1 gana" : fuerzaJ2 > fuerzaJ1 ? "J2 gana" : "Empate",
                liderHabJ1, liderUsadoJ1, liderHabJ2, liderUsadoJ2, dobleEspias);

        Ronda ronda = new Ronda();
        ronda.setPartida(partida);
        ronda.setNumeroRonda(partida.getRondaActual());
        ronda.setPuntajeJugadorUno(fuerzaJ1);
        ronda.setPuntajeJugadorDos(fuerzaJ2);

        if (fuerzaJ1 > fuerzaJ2) {
            ronda.setGanadorId(partida.getJugadorUnoId());
            partida.setVidasJugadorDos(partida.getVidasJugadorDos() - 1);
            log.info("[P-{}] J2 pierde 1 vida → vidasJ1={} vidasJ2={}",
                    partida.getId(), partida.getVidasJugadorUno(), partida.getVidasJugadorDos());
        } else if (fuerzaJ2 > fuerzaJ1) {
            ronda.setGanadorId(partida.getJugadorDosId());
            partida.setVidasJugadorUno(partida.getVidasJugadorUno() - 1);
            log.info("[P-{}] J1 pierde 1 vida → vidasJ1={} vidasJ2={}",
                    partida.getId(), partida.getVidasJugadorUno(), partida.getVidasJugadorDos());
        } else {
            ronda.setEmpate(true);
            partida.setVidasJugadorUno(partida.getVidasJugadorUno() - 1);
            partida.setVidasJugadorDos(partida.getVidasJugadorDos() - 1);
            log.info("[P-{}] Empate de ronda → ambos pierden 1 vida → vidasJ1={} vidasJ2={}",
                    partida.getId(), partida.getVidasJugadorUno(), partida.getVidasJugadorDos());
        }

        rondaRepo.save(ronda);
        partida.getRondas().add(ronda);

        campoJ1.forEach(cartaStateMachine::descartar);
        campoJ2.forEach(cartaStateMachine::descartar);
        cartaPartidaRepo.saveAll(campoJ1);
        cartaPartidaRepo.saveAll(campoJ2);

        partida.setJugadorUnoPaso(false);
        partida.setJugadorDosPaso(false);

        if (partida.getVidasJugadorUno() <= 0 && partida.getVidasJugadorDos() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setEmpate(true);
            partida.setFinishedAt(LocalDateTime.now());
            log.info("[P-{}] Partida TERMINADA — Empate", partida.getId());
            return;
        }
        if (partida.getVidasJugadorUno() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setGanadorId(partida.getJugadorDosId());
            partida.setFinishedAt(LocalDateTime.now());
            log.info("[P-{}] Partida TERMINADA — Ganador: J2={}", partida.getId(), partida.getJugadorDosId());
            return;
        }
        if (partida.getVidasJugadorDos() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setGanadorId(partida.getJugadorUnoId());
            partida.setFinishedAt(LocalDateTime.now());
            log.info("[P-{}] Partida TERMINADA — Ganador: J1={}", partida.getId(), partida.getJugadorUnoId());
            return;
        }
        if (partida.getRondaActual() >= 3) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setFinishedAt(LocalDateTime.now());
            if (partida.getVidasJugadorUno() > partida.getVidasJugadorDos()) {
                partida.setGanadorId(partida.getJugadorUnoId());
                log.info("[P-{}] Partida TERMINADA (3 rondas) — Ganador: J1={}", partida.getId(), partida.getJugadorUnoId());
            } else if (partida.getVidasJugadorDos() > partida.getVidasJugadorUno()) {
                partida.setGanadorId(partida.getJugadorDosId());
                log.info("[P-{}] Partida TERMINADA (3 rondas) — Ganador: J2={}", partida.getId(), partida.getJugadorDosId());
            } else {
                partida.setEmpate(true);
                log.info("[P-{}] Partida TERMINADA (3 rondas) — Empate", partida.getId());
            }
            return;
        }

        int cartasARobar = partida.getRondaActual() == 1 ? ROBAR_TRAS_RONDA_1 : ROBAR_TRAS_RONDA_2;
        habilidadService.robarCartas(partida, partida.getJugadorUnoId(), cartasARobar);
        habilidadService.robarCartas(partida, partida.getJugadorDosId(), cartasARobar);

        partida.setRondaActual(partida.getRondaActual() + 1);

        if (ronda.getGanadorId() != null) {
            partida.setTurnoJugadorId(ronda.getGanadorId());
        } else {
            partida.setTurnoJugadorId(random.nextBoolean() ? partida.getJugadorUnoId() : partida.getJugadorDosId());
        }
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

    private void notificarResultado(Partida partida) {
        try {
            jugadorClient.reportMatchResult(
                    partida.getJugadorUnoId(),
                    partida.getJugadorDosId(),
                    partida.getGanadorId(),
                    partida.isEmpate()
            );
        } catch (Exception e) {
            log.warn("Failed to update player stats for match {}: {}", partida.getId(), e.getMessage());
        }
    }

    private HabilidadCarta getLiderHabilidadSafe(Mazo mazo) {
        return (mazo.getLider() != null) ? mazo.getLider().getHabilidad() : HabilidadCarta.NINGUNA;
    }

    // ==================== VALIDATION HELPERS ====================

    private Partida findPartida(Long partidaId) {
        return partidaRepo.findById(partidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
    }

    private void validateJugadorEnPartida(UUID jugadorId, Partida partida) {
        if (!jugadorId.equals(partida.getJugadorUnoId()) && !jugadorId.equals(partida.getJugadorDosId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
    }

    private void validateTurno(UUID jugadorId, Partida partida) {
        if (partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not in progress");
        }
        if (!jugadorId.equals(partida.getTurnoJugadorId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn");
        }
    }

    // ==================== DTO BUILDER ====================

    private PartidaDTO buildPartidaDTO(Partida partida, UUID viewerId) {
        boolean viewerEsJ1 = viewerId.equals(partida.getJugadorUnoId());
        UUID myId = viewerEsJ1 ? partida.getJugadorUnoId() : partida.getJugadorDosId();
        UUID opId = viewerEsJ1 ? partida.getJugadorDosId() : partida.getJugadorUnoId();

        List<CartaPartida> misCartas = cartaPartidaRepo.findByPartidaAndJugadorId(partida, myId);
        List<CartaPartida> cartasOponente = cartaPartidaRepo.findByPartidaAndJugadorId(partida, opId);

        List<CartaPartida> miCampo = misCartas.stream().filter(c -> c.getZona() == ZonaCarta.CAMPO).toList();
        List<CartaPartida> campoOponente = cartasOponente.stream().filter(c -> c.getZona() == ZonaCarta.CAMPO).toList();

        Set<FilaCarta> climaActivo = habilidadService.resolverClimaActivo(miCampo, campoOponente);

        List<CartaPartidaDTO> climaEnCampo = Stream.concat(miCampo.stream(), campoOponente.stream())
                .filter(c -> c.getCartaCatalogo().getTipo() == TipoCarta.CLIMA)
                .map(CartaPartidaDTO::from)
                .toList();

        // Leader context
        HabilidadCarta miLiderHab     = getLiderHabilidadSafe(viewerEsJ1 ? partida.getMazoJugadorUno() : partida.getMazoJugadorDos());
        boolean        miLiderUsado   = viewerEsJ1 ? partida.isLiderUsadoJugadorUno() : partida.isLiderUsadoJugadorDos();
        HabilidadCarta opLiderHab     = getLiderHabilidadSafe(viewerEsJ1 ? partida.getMazoJugadorDos() : partida.getMazoJugadorUno());
        boolean        opLiderUsado   = viewerEsJ1 ? partida.isLiderUsadoJugadorDos() : partida.isLiderUsadoJugadorUno();

        // Revealed cards (Emhyr Emperador): viewer's revealed cards are shown in oponente section
        String reveladasStr = viewerEsJ1 ? partida.getCartasReveladasJ1() : partida.getCartasReveladasJ2();
        List<Long> reveladasIds = parseIds(reveladasStr);
        List<CartaPartidaDTO> cartasReveladasOponente = cartasOponente.stream()
                .filter(c -> c.getZona() == ZonaCarta.MANO && reveladasIds.contains(c.getId()))
                .map(CartaPartidaDTO::from)
                .toList();

        JugadorPartidaDTO yo = buildJugadorDTO(myId, misCartas, miCampo, partida, viewerEsJ1,
                true, climaActivo, miLiderHab, miLiderUsado, List.of());
        JugadorPartidaDTO oponente = buildJugadorDTO(opId, cartasOponente, campoOponente, partida, !viewerEsJ1,
                false, climaActivo, opLiderHab, opLiderUsado, cartasReveladasOponente);

        List<RondaDTO> rondasDTO = partida.getRondas().stream()
                .map(r -> {
                    int miPuntaje = viewerEsJ1 ? r.getPuntajeJugadorUno() : r.getPuntajeJugadorDos();
                    int puntajeOp = viewerEsJ1 ? r.getPuntajeJugadorDos() : r.getPuntajeJugadorUno();
                    return RondaDTO.builder()
                            .numeroRonda(r.getNumeroRonda())
                            .miPuntaje(miPuntaje)
                            .puntajeOponente(puntajeOp)
                            .ganadorId(r.getGanadorId())
                            .empate(r.isEmpate())
                            .build();
                })
                .toList();

        return PartidaDTO.builder()
                .id(partida.getId())
                .estado(partida.getEstado())
                .rondaActual(partida.getRondaActual())
                .esMiTurno(viewerId.equals(partida.getTurnoJugadorId()))
                .yo(yo)
                .oponente(oponente)
                .rondas(rondasDTO)
                .climaEnCampo(climaEnCampo)
                .ganadorId(partida.getGanadorId())
                .empate(partida.isEmpate())
                .createdAt(partida.getCreatedAt())
                .finishedAt(partida.getFinishedAt())
                .build();
    }

    private JugadorPartidaDTO buildJugadorDTO(UUID jugadorId, List<CartaPartida> cartas,
                                               List<CartaPartida> campo, Partida partida,
                                               boolean esJ1, boolean esMio,
                                               Set<FilaCarta> climaActivo,
                                               HabilidadCarta liderHab, boolean liderUsado,
                                               List<CartaPartidaDTO> cartasReveladas) {
        List<CartaPartida> mano = cartas.stream().filter(c -> c.getZona() == ZonaCarta.MANO).toList();
        List<CartaPartida> mazo = cartas.stream().filter(c -> c.getZona() == ZonaCarta.MAZO).toList();
        List<CartaPartida> cementerio = cartas.stream().filter(c -> c.getZona() == ZonaCarta.CEMENTERIO).toList();

        TableroDTO tablero = buildTableroDTO(campo, climaActivo, liderHab, liderUsado);
        int fuerzaTotal = tablero.getFuerzaCuerpoACuerpo() + tablero.getFuerzaDistancia() + tablero.getFuerzaAsedio();

        Mazo mazoEntity = esJ1 ? partida.getMazoJugadorUno() : partida.getMazoJugadorDos();
        CartaCatalogoDTO liderDTO = mazoEntity.getLider() != null
                ? CartaCatalogoDTO.from(mazoEntity.getLider()) : null;

        return JugadorPartidaDTO.builder()
                .jugadorId(jugadorId)
                .vidas(esJ1 ? partida.getVidasJugadorUno() : partida.getVidasJugadorDos())
                .paso(esJ1 ? partida.isJugadorUnoPaso() : partida.isJugadorDosPaso())
                .mulliganCompleto(esJ1 ? partida.isJugadorUnoMulligan() : partida.isJugadorDosMulligan())
                .mano(esMio ? mano.stream().map(CartaPartidaDTO::from).toList() : null)
                .manoCount(mano.size())
                .mazoCount(mazo.size())
                .tablero(tablero)
                .cementerio(cementerio.stream().map(CartaPartidaDTO::from).toList())
                .fuerzaTotal(fuerzaTotal)
                .lider(liderDTO)
                .liderUsado(liderUsado)
                .cartasReveladas(esMio ? null : (cartasReveladas.isEmpty() ? null : cartasReveladas))
                .build();
    }

    private TableroDTO buildTableroDTO(List<CartaPartida> campo, Set<FilaCarta> climaActivo,
                                        HabilidadCarta liderHab, boolean liderUsado) {
        HabilidadCarta hab     = liderUsado ? liderHab : HabilidadCarta.NINGUNA;
        boolean dobleCac       = (hab == HabilidadCarta.LIDER_DOUBLE_CAC);
        boolean dobleDistancia = (hab == HabilidadCarta.LIDER_DOUBLE_RANGED);
        boolean dobleAsedio    = (hab == HabilidadCarta.LIDER_DOUBLE_SIEGE);
        boolean medioClima     = (hab == HabilidadCarta.LIDER_HALF_WEATHER);

        List<CartaPartida> cac    = habilidadService.filtrarFilaUnidades(campo, FilaCarta.CUERPO_A_CUERPO);
        List<CartaPartida> dist   = habilidadService.filtrarFilaUnidades(campo, FilaCarta.DISTANCIA);
        List<CartaPartida> asedio = habilidadService.filtrarFilaUnidades(campo, FilaCarta.ASEDIO);

        return TableroDTO.builder()
                .cuerpoACuerpo(cac.stream().map(CartaPartidaDTO::from).toList())
                .distancia(dist.stream().map(CartaPartidaDTO::from).toList())
                .asedio(asedio.stream().map(CartaPartidaDTO::from).toList())
                .fuerzaCuerpoACuerpo(habilidadService.calcularFilaPublica(cac,    climaActivo, FilaCarta.CUERPO_A_CUERPO,
                        habilidadService.tieneHorn(campo, FilaCarta.CUERPO_A_CUERPO), dobleCac,       medioClima))
                .fuerzaDistancia    (habilidadService.calcularFilaPublica(dist,   climaActivo, FilaCarta.DISTANCIA,
                        habilidadService.tieneHorn(campo, FilaCarta.DISTANCIA),       dobleDistancia, medioClima))
                .fuerzaAsedio       (habilidadService.calcularFilaPublica(asedio, climaActivo, FilaCarta.ASEDIO,
                        habilidadService.tieneHorn(campo, FilaCarta.ASEDIO),          dobleAsedio,    medioClima))
                .slotLateralCuerpoACuerpo(slotLateral(campo, FilaCarta.CUERPO_A_CUERPO))
                .slotLateralDistancia    (slotLateral(campo, FilaCarta.DISTANCIA))
                .slotLateralAsedio       (slotLateral(campo, FilaCarta.ASEDIO))
                .build();
    }

    private CartaPartidaDTO slotLateral(List<CartaPartida> campo, FilaCarta fila) {
        return campo.stream()
                .filter(cp -> cp.isEsSlotLateral() && cp.getFilaEnCampo() == fila)
                .findFirst()
                .map(CartaPartidaDTO::from)
                .orElse(null);
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
