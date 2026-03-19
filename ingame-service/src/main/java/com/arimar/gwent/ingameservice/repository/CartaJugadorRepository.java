package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.entity.CartaJugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartaJugadorRepository extends JpaRepository<CartaJugador, Long> {
    List<CartaJugador> findByJugadorId(UUID jugadorId);
    List<CartaJugador> findByJugadorIdAndCartaCatalogoIdIn(UUID jugadorId, List<Long> cartaIds);
}
