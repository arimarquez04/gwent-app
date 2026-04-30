package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.domain.enums.EstadoPartida;
import com.arimar.gwent.ingameservice.entity.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    @Query("SELECT p FROM Partida p WHERE p.estado <> :estado AND (p.jugadorUnoId = :jugadorId OR p.jugadorDosId = :jugadorId)")
    List<Partida> findActiveByJugadorId(@Param("estado") EstadoPartida estado, @Param("jugadorId") UUID jugadorId);
}
