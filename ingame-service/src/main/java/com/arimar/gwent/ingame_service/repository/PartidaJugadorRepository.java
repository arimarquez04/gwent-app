package com.arimar.gwent.ingame_service.repository;


import com.arimar.gwent.ingame_service.model.PartidaJugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartidaJugadorRepository extends JpaRepository<PartidaJugador, Integer> {

    @Query(value = "SELECT * FROM `partida_jugador` WHERE `partida_id` = ?1", nativeQuery = true)
    List<PartidaJugador> traerPartidasJugadorDeUnaPartida(Integer partidaId);
}
