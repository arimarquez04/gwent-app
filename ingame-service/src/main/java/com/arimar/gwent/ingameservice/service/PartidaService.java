package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.*;
import com.arimar.gwent.ingameservice.dto.*;
import com.arimar.gwent.ingameservice.entity.*;
import com.arimar.gwent.ingameservice.repository.CartaPartidaRepository;
import com.arimar.gwent.ingameservice.repository.MazoRepository;
import com.arimar.gwent.ingameservice.repository.PartidaRepository;
import com.arimar.gwent.ingameservice.repository.RondaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

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

            cartaADevolver.setZona(ZonaCarta.MAZO);
            mano.remove(cartaADevolver);
            mazo.add(cartaADevolver);

            if (!mazo.isEmpty()) {
                CartaPartida cartaRobada = mazo.get(random.nextInt(mazo.size()));
                cartaRobada.setZona(ZonaCarta.MANO);
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
        if (carta.getTipo() != TipoCarta.UNIDAD) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only unit cards can be played in this version");
        }

        FilaCarta filaDestino = determinarFila(carta, req.getFila());

        cartaPartida.setZona(ZonaCarta.CAMPO);
        cartaPartida.setFilaEnCampo(filaDestino);
        cartaPartidaRepo.save(cartaPartida);

        // Turn logic
        UUID oponenteId = esJugadorUno ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        boolean oponentePaso = esJugadorUno ? partida.isJugadorDosPaso() : partida.isJugadorUnoPaso();

        // Auto-pass if no cards left in hand
        List<CartaPartida> manoRestante = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MANO);
        if (manoRestante.isEmpty()) {
            if (esJugadorUno) {
                partida.setJugadorUnoPaso(true);
            } else {
                partida.setJugadorDosPaso(true);
            }
        }

        boolean jugadorPaso = esJugadorUno ? partida.isJugadorUnoPaso() : partida.isJugadorDosPaso();

        if (jugadorPaso && oponentePaso) {
            resolverRonda(partida);
        } else if (!oponentePaso) {
            partida.setTurnoJugadorId(oponenteId);
        }
        // If opponent passed, keep turn (player continues playing)

        partidaRepo.save(partida);
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

        if (esJugadorUno) {
            partida.setJugadorUnoPaso(true);
        } else {
            partida.setJugadorDosPaso(true);
        }

        UUID oponenteId = esJugadorUno ? partida.getJugadorDosId() : partida.getJugadorUnoId();
        boolean oponentePaso = esJugadorUno ? partida.isJugadorDosPaso() : partida.isJugadorUnoPaso();

        if (oponentePaso) {
            resolverRonda(partida);
        } else {
            partida.setTurnoJugadorId(oponenteId);
        }

        partidaRepo.save(partida);
        return buildPartidaDTO(partida, jugadorId);
    }

    // ==================== INTERNAL METHODS ====================

    private void repartirCartas(Partida partida, UUID jugadorId, Mazo mazo) {
        List<CartaPartida> todasLasCartas = new ArrayList<>();

        for (MazoCarta mc : mazo.getCartas()) {
            if (mc.getCartaCatalogo().getTipo() == TipoCarta.LIDER) {
                continue;
            }
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
        for (int i = 0; i < aDar; i++) {
            todasLasCartas.get(i).setZona(ZonaCarta.MANO);
        }

        cartaPartidaRepo.saveAll(todasLasCartas);
    }

    private void resolverRonda(Partida partida) {
        List<CartaPartida> campoJ1 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorUnoId(), ZonaCarta.CAMPO);
        List<CartaPartida> campoJ2 = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(
                partida, partida.getJugadorDosId(), ZonaCarta.CAMPO);

        int fuerzaJ1 = campoJ1.stream()
                .filter(cp -> cp.getCartaCatalogo().getFuerza() != null)
                .mapToInt(cp -> cp.getCartaCatalogo().getFuerza())
                .sum();
        int fuerzaJ2 = campoJ2.stream()
                .filter(cp -> cp.getCartaCatalogo().getFuerza() != null)
                .mapToInt(cp -> cp.getCartaCatalogo().getFuerza())
                .sum();

        // Create round record
        Ronda ronda = new Ronda();
        ronda.setPartida(partida);
        ronda.setNumeroRonda(partida.getRondaActual());
        ronda.setPuntajeJugadorUno(fuerzaJ1);
        ronda.setPuntajeJugadorDos(fuerzaJ2);

        if (fuerzaJ1 > fuerzaJ2) {
            ronda.setGanadorId(partida.getJugadorUnoId());
            partida.setVidasJugadorDos(partida.getVidasJugadorDos() - 1);
        } else if (fuerzaJ2 > fuerzaJ1) {
            ronda.setGanadorId(partida.getJugadorDosId());
            partida.setVidasJugadorUno(partida.getVidasJugadorUno() - 1);
        } else {
            ronda.setEmpate(true);
            partida.setVidasJugadorUno(partida.getVidasJugadorUno() - 1);
            partida.setVidasJugadorDos(partida.getVidasJugadorDos() - 1);
        }

        rondaRepo.save(ronda);
        partida.getRondas().add(ronda);

        // Move all field cards to graveyard
        for (CartaPartida cp : campoJ1) {
            cp.setZona(ZonaCarta.CEMENTERIO);
            cp.setFilaEnCampo(null);
        }
        for (CartaPartida cp : campoJ2) {
            cp.setZona(ZonaCarta.CEMENTERIO);
            cp.setFilaEnCampo(null);
        }
        cartaPartidaRepo.saveAll(campoJ1);
        cartaPartidaRepo.saveAll(campoJ2);

        // Reset pass flags
        partida.setJugadorUnoPaso(false);
        partida.setJugadorDosPaso(false);

        // Check end of match
        if (partida.getVidasJugadorUno() <= 0 && partida.getVidasJugadorDos() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setEmpate(true);
            partida.setFinishedAt(LocalDateTime.now());
            return;
        }
        if (partida.getVidasJugadorUno() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setGanadorId(partida.getJugadorDosId());
            partida.setFinishedAt(LocalDateTime.now());
            return;
        }
        if (partida.getVidasJugadorDos() <= 0) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setGanadorId(partida.getJugadorUnoId());
            partida.setFinishedAt(LocalDateTime.now());
            return;
        }
        if (partida.getRondaActual() >= 3) {
            partida.setEstado(EstadoPartida.TERMINADA);
            partida.setFinishedAt(LocalDateTime.now());
            if (partida.getVidasJugadorUno() > partida.getVidasJugadorDos()) {
                partida.setGanadorId(partida.getJugadorUnoId());
            } else if (partida.getVidasJugadorDos() > partida.getVidasJugadorUno()) {
                partida.setGanadorId(partida.getJugadorDosId());
            } else {
                partida.setEmpate(true);
            }
            return;
        }

        // Draw cards for next round
        int cartasARobar = partida.getRondaActual() == 1 ? ROBAR_TRAS_RONDA_1 : ROBAR_TRAS_RONDA_2;
        robarCartas(partida, partida.getJugadorUnoId(), cartasARobar);
        robarCartas(partida, partida.getJugadorDosId(), cartasARobar);

        // Next round
        partida.setRondaActual(partida.getRondaActual() + 1);

        // Winner of round goes first; on tie, random
        if (ronda.getGanadorId() != null) {
            partida.setTurnoJugadorId(ronda.getGanadorId());
        } else {
            partida.setTurnoJugadorId(random.nextBoolean() ? partida.getJugadorUnoId() : partida.getJugadorDosId());
        }
    }

    private void robarCartas(Partida partida, UUID jugadorId, int cantidad) {
        List<CartaPartida> mazo = cartaPartidaRepo.findByPartidaAndJugadorIdAndZona(partida, jugadorId, ZonaCarta.MAZO);
        Collections.shuffle(mazo, random);
        int aRobar = Math.min(cantidad, mazo.size());
        for (int i = 0; i < aRobar; i++) {
            mazo.get(i).setZona(ZonaCarta.MANO);
        }
        if (aRobar > 0) {
            cartaPartidaRepo.saveAll(mazo.subList(0, aRobar));
        }
    }

    private FilaCarta determinarFila(CartaCatalogo carta, FilaCarta filaRequest) {
        if (carta.getFila() == FilaCarta.AGIL) {
            if (filaRequest == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Agile cards require a row selection (CUERPO_A_CUERPO or DISTANCIA)");
            }
            if (filaRequest != FilaCarta.CUERPO_A_CUERPO && filaRequest != FilaCarta.DISTANCIA) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Agile cards can only be placed on CUERPO_A_CUERPO or DISTANCIA");
            }
            return filaRequest;
        }
        return carta.getFila();
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

        JugadorPartidaDTO yo = buildJugadorDTO(myId, misCartas, partida, viewerEsJ1, true);
        JugadorPartidaDTO oponente = buildJugadorDTO(opId, cartasOponente, partida, !viewerEsJ1, false);

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
                .ganadorId(partida.getGanadorId())
                .empate(partida.isEmpate())
                .createdAt(partida.getCreatedAt())
                .finishedAt(partida.getFinishedAt())
                .build();
    }

    private JugadorPartidaDTO buildJugadorDTO(UUID jugadorId, List<CartaPartida> cartas,
                                               Partida partida, boolean esJ1, boolean esMio) {
        List<CartaPartida> mano = cartas.stream().filter(c -> c.getZona() == ZonaCarta.MANO).toList();
        List<CartaPartida> mazo = cartas.stream().filter(c -> c.getZona() == ZonaCarta.MAZO).toList();
        List<CartaPartida> campo = cartas.stream().filter(c -> c.getZona() == ZonaCarta.CAMPO).toList();
        List<CartaPartida> cementerio = cartas.stream().filter(c -> c.getZona() == ZonaCarta.CEMENTERIO).toList();

        TableroDTO tablero = buildTableroDTO(campo);

        int fuerzaTotal = tablero.getFuerzaCuerpoACuerpo() + tablero.getFuerzaDistancia() + tablero.getFuerzaAsedio();

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
                .build();
    }

    private TableroDTO buildTableroDTO(List<CartaPartida> campo) {
        List<CartaPartida> cac = campo.stream().filter(c -> c.getFilaEnCampo() == FilaCarta.CUERPO_A_CUERPO).toList();
        List<CartaPartida> dist = campo.stream().filter(c -> c.getFilaEnCampo() == FilaCarta.DISTANCIA).toList();
        List<CartaPartida> asedio = campo.stream().filter(c -> c.getFilaEnCampo() == FilaCarta.ASEDIO).toList();

        return TableroDTO.builder()
                .cuerpoACuerpo(cac.stream().map(CartaPartidaDTO::from).toList())
                .distancia(dist.stream().map(CartaPartidaDTO::from).toList())
                .asedio(asedio.stream().map(CartaPartidaDTO::from).toList())
                .fuerzaCuerpoACuerpo(sumarFuerza(cac))
                .fuerzaDistancia(sumarFuerza(dist))
                .fuerzaAsedio(sumarFuerza(asedio))
                .build();
    }

    private int sumarFuerza(List<CartaPartida> cartas) {
        return cartas.stream()
                .filter(cp -> cp.getCartaCatalogo().getFuerza() != null)
                .mapToInt(cp -> cp.getCartaCatalogo().getFuerza())
                .sum();
    }
}
