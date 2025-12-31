package com.arimar.gwent.ingame_service.repository;

import com.arimar.gwent.ingame_service.model.PartidaRonda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface PartidaRondaRepository extends JpaRepository<PartidaRonda, Integer> {
    @Query(value = "Select * from partida_ronda where partida_id = ?1 and numero_ronda = ?2", nativeQuery = true)
    PartidaRonda traerPartidaRondaDeUnaPartida(Integer partidaID, Integer numeroRonda);

}
