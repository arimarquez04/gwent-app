package com.arimar.gwent.ingame_service.repository;

import com.arimar.gwent.ingame_service.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Integer> {

}
