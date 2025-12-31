package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.model.Partida;
import com.arimar.gwent.ingame_service.model.PartidaJugador;
import com.arimar.gwent.ingame_service.model.PartidaRonda;
import com.arimar.gwent.ingame_service.repository.PartidaRondaRepository;
import org.springframework.stereotype.Service;

@Service
public class PartidaRondaServiceImpl implements PartidaRondaService{
    PartidaRondaRepository partidaRondaRepository;
    public PartidaRondaServiceImpl(PartidaRondaRepository partidaRondaRepository){
        this.partidaRondaRepository=partidaRondaRepository;
    }
    @Override
    public PartidaRonda crearPartidaRonda(Partida partida, int numeroRonda, PartidaJugador partidaJugadorUno, PartidaJugador partidaJugadorDos){
        PartidaRonda partidaRonda = new PartidaRonda();
        partidaRonda.setPartida(partida);
        partidaRonda.setNumeroRonda(numeroRonda);
        partidaRonda.setPartidaJugadorUno(partidaJugadorUno);
        partidaRonda.setPartidaJugadordos(partidaJugadorDos);
        return partidaRondaRepository.save(partidaRonda);
    }
    @Override
    public PartidaRonda guardarPartidaRonda(PartidaRonda partidaRonda){
        return partidaRondaRepository.save(partidaRonda);
    }
    @Override
    public PartidaRonda traerPartidaRondaDeUnaPartida(Partida partida, int ronda) {
        return partidaRondaRepository.traerPartidaRondaDeUnaPartida(partida.getId(), ronda);
    }

}
