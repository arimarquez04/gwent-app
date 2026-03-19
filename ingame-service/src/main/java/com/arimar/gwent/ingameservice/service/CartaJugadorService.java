package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.dto.CartaJugadorDTO;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import com.arimar.gwent.ingameservice.entity.CartaJugador;
import com.arimar.gwent.ingameservice.repository.CartaCatalogoRepository;
import com.arimar.gwent.ingameservice.repository.CartaJugadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartaJugadorService {

    private final CartaJugadorRepository cartaJugadorRepo;
    private final CartaCatalogoRepository cartaCatalogoRepo;

    public List<CartaJugadorDTO> unlockCards(UUID jugadorId, List<Long> cardIds) {
        List<CartaCatalogo> cartas = cartaCatalogoRepo.findAllById(cardIds);
        if (cartas.size() != cardIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more cards not found in catalog");
        }

        Map<Long, CartaJugador> existingMap = cartaJugadorRepo
                .findByJugadorIdAndCartaCatalogoIdIn(jugadorId, cardIds)
                .stream()
                .collect(Collectors.toMap(cj -> cj.getCartaCatalogo().getId(), cj -> cj));

        List<CartaJugador> toSave = new ArrayList<>();
        for (CartaCatalogo carta : cartas) {
            CartaJugador existing = existingMap.get(carta.getId());
            if (existing == null) {
                CartaJugador cj = new CartaJugador();
                cj.setJugadorId(jugadorId);
                cj.setCartaCatalogo(carta);
                cj.setCantidad(1);
                toSave.add(cj);
            } else if (existing.getCantidad() < carta.getMaxCopias()) {
                existing.setCantidad(existing.getCantidad() + 1);
                toSave.add(existing);
            }
            // cantidad == maxCopias → skip silenciosamente
        }

        return cartaJugadorRepo.saveAll(toSave).stream()
                .map(CartaJugadorDTO::from)
                .toList();
    }

    public List<CartaJugadorDTO> getMyCards(UUID jugadorId) {
        return cartaJugadorRepo.findByJugadorId(jugadorId).stream()
                .map(CartaJugadorDTO::from)
                .toList();
    }
}
