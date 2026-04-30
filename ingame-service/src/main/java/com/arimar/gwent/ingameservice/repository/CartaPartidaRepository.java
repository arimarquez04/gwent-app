package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.domain.enums.ZonaCarta;
import com.arimar.gwent.ingameservice.entity.CartaPartida;
import com.arimar.gwent.ingameservice.entity.Partida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartaPartidaRepository extends JpaRepository<CartaPartida, Long> {

    List<CartaPartida> findByPartidaAndJugadorId(Partida partida, UUID jugadorId);

    List<CartaPartida> findByPartidaAndJugadorIdAndZona(Partida partida, UUID jugadorId, ZonaCarta zona);
}
