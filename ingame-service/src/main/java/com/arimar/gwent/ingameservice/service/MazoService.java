package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.EstadoMazo;
import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import com.arimar.gwent.ingameservice.dto.*;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import com.arimar.gwent.ingameservice.entity.CartaJugador;
import com.arimar.gwent.ingameservice.entity.Mazo;
import com.arimar.gwent.ingameservice.entity.MazoCarta;
import com.arimar.gwent.ingameservice.repository.CartaCatalogoRepository;
import com.arimar.gwent.ingameservice.repository.CartaJugadorRepository;
import com.arimar.gwent.ingameservice.repository.MazoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MazoService {

    private static final int MAX_MAZOS_POR_FACCION = 3;
    private static final int MIN_UNIDADES_PARA_ACTIVAR = 22;

    private final MazoRepository mazoRepo;
    private final CartaCatalogoRepository catalogoRepo;
    private final CartaJugadorRepository cartaJugadorRepo;

    @Transactional
    public MazoDTO createMazo(UUID jugadorId, CreateMazoRequest req) {
        if (mazoRepo.countByJugadorIdAndFaccion(jugadorId, req.getFaccion()) >= MAX_MAZOS_POR_FACCION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Deck limit reached for faction " + req.getFaccion());
        }

        Mazo mazo = new Mazo();
        mazo.setJugadorId(jugadorId);
        mazo.setFaccion(req.getFaccion());
        mazo.setNombre(req.getNombre());
        mazo.setEstado(EstadoMazo.INACTIVO);

        if (req.getLiderId() != null) {
            CartaCatalogo lider = validateLider(jugadorId, req.getFaccion(), req.getLiderId());
            mazo.setLider(lider);
        }

        if (!req.getCardEntries().isEmpty()) {
            validateCartas(jugadorId, req.getFaccion(), req.getCardEntries());
            addCartasToMazo(mazo, req.getCardEntries());
        }

        return MazoDTO.from(mazoRepo.save(mazo));
    }

    public List<MazoDTO> getMazos(UUID jugadorId) {
        return mazoRepo.findByJugadorId(jugadorId).stream()
                .map(MazoDTO::from)
                .toList();
    }

    public MazoDTO getMazoById(UUID jugadorId, Long mazoId) {
        return MazoDTO.from(findOwnedMazo(jugadorId, mazoId));
    }

    @Transactional
    public MazoDTO updateMazo(UUID jugadorId, Long mazoId, UpdateMazoRequest req) {
        Mazo mazo = findOwnedMazo(jugadorId, mazoId);

        if (req.getNombre() != null) {
            mazo.setNombre(req.getNombre());
        }

        if (req.getLiderId() != null) {
            if (req.getLiderId() == -1L) {
                mazo.setLider(null);
            } else {
                CartaCatalogo lider = validateLider(jugadorId, mazo.getFaccion(), req.getLiderId());
                mazo.setLider(lider);
            }
        }

        if (req.getCardEntries() != null) {
            validateCartas(jugadorId, mazo.getFaccion(), req.getCardEntries());
            mazo.getCartas().clear();
            addCartasToMazo(mazo, req.getCardEntries());
        }

        return MazoDTO.from(mazoRepo.save(mazo));
    }

    @Transactional
    public MazoDTO setActiveMazo(UUID jugadorId, Long mazoId) {
        Mazo mazo = findOwnedMazo(jugadorId, mazoId);

        int totalUnidades = mazo.getCartas().stream()
                .filter(mc -> mc.getCartaCatalogo().getTipo() == TipoCarta.UNIDAD)
                .mapToInt(MazoCarta::getCantidad)
                .sum();

        if (totalUnidades < MIN_UNIDADES_PARA_ACTIVAR) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Deck needs at least " + MIN_UNIDADES_PARA_ACTIVAR + " unit cards to be activated (has " + totalUnidades + ")");
        }

        mazoRepo.findByJugadorIdAndEstadoAndFaccion(jugadorId, EstadoMazo.ACTIVO, mazo.getFaccion())
                .ifPresent(active -> {
                    active.setEstado(EstadoMazo.INACTIVO);
                    mazoRepo.save(active);
                });

        mazo.setEstado(EstadoMazo.ACTIVO);
        return MazoDTO.from(mazoRepo.save(mazo));
    }

    @Transactional
    public void deleteMazo(UUID jugadorId, Long mazoId) {
        Mazo mazo = findOwnedMazo(jugadorId, mazoId);
        mazoRepo.delete(mazo);
    }

    // --- private helpers ---

    private Mazo findOwnedMazo(UUID jugadorId, Long mazoId) {
        Mazo mazo = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
        if (!mazo.getJugadorId().equals(jugadorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found");
        }
        return mazo;
    }

    private void validateCartas(UUID jugadorId, Faccion faccionMazo, List<MazoCartaEntrada> entries) {
        List<Long> ids = entries.stream().map(MazoCartaEntrada::getCartaCatalogoId).toList();
        List<CartaCatalogo> cartas = catalogoRepo.findAllById(ids);

        if (cartas.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more cards not found in catalog");
        }

        Map<Long, CartaCatalogo> cartaMap = cartas.stream()
                .collect(Collectors.toMap(CartaCatalogo::getId, c -> c));

        for (CartaCatalogo carta : cartas) {
            if (carta.getTipo() == TipoCarta.LIDER) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Leaders must be set in the lider field, not as cards: " + carta.getNombre());
            }
            if (carta.getFaccion() != faccionMazo && carta.getFaccion() != Faccion.NEUTRAL) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Card '" + carta.getNombre() + "' does not belong to faction " + faccionMazo);
            }
        }

        Map<Long, CartaJugador> unlockedMap = cartaJugadorRepo
                .findByJugadorIdAndCartaCatalogoIdIn(jugadorId, ids)
                .stream()
                .collect(Collectors.toMap(cj -> cj.getCartaCatalogo().getId(), cj -> cj));

        for (MazoCartaEntrada entry : entries) {
            if (!unlockedMap.containsKey(entry.getCartaCatalogoId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Card '" + cartaMap.get(entry.getCartaCatalogoId()).getNombre() + "' is not unlocked");
            }
        }
    }

    private CartaCatalogo validateLider(UUID jugadorId, Faccion faccionMazo, Long liderId) {
        CartaCatalogo lider = catalogoRepo.findById(liderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leader card not found"));

        if (lider.getTipo() != TipoCarta.LIDER) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card '" + lider.getNombre() + "' is not a leader");
        }
        if (lider.getFaccion() != faccionMazo) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Leader '" + lider.getNombre() + "' does not belong to faction " + faccionMazo);
        }

        boolean unlocked = cartaJugadorRepo
                .findByJugadorIdAndCartaCatalogoIdIn(jugadorId, List.of(liderId))
                .stream().findFirst().isPresent();
        if (!unlocked) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Leader '" + lider.getNombre() + "' is not unlocked");
        }

        return lider;
    }

    private void addCartasToMazo(Mazo mazo, List<MazoCartaEntrada> entries) {
        List<Long> ids = entries.stream().map(MazoCartaEntrada::getCartaCatalogoId).toList();
        Map<Long, CartaCatalogo> cartaMap = catalogoRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(CartaCatalogo::getId, c -> c));

        for (MazoCartaEntrada entry : entries) {
            MazoCarta mc = new MazoCarta();
            mc.setMazo(mazo);
            mc.setCartaCatalogo(cartaMap.get(entry.getCartaCatalogoId()));
            mc.setCantidad(entry.getCantidad());
            mazo.getCartas().add(mc);
        }
    }
}
