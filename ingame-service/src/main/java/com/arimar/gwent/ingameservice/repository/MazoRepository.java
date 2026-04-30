package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.domain.enums.EstadoMazo;
import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.entity.Mazo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MazoRepository extends JpaRepository<Mazo, Long> {

    List<Mazo> findByJugadorId(UUID jugadorId);

    List<Mazo> findByJugadorIdAndFaccion(UUID jugadorId, Faccion faccion);

    long countByJugadorIdAndFaccion(UUID jugadorId, Faccion faccion);

    Optional<Mazo> findByJugadorIdAndEstadoAndFaccion(UUID jugadorId, EstadoMazo estado, Faccion faccion);
}
