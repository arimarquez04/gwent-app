package com.arimar.gwent.ingame_service.service.impl;

import com.arimar.gwent.ingame_service.model.Jugador;
import com.arimar.gwent.ingame_service.model.Partida;
import com.arimar.gwent.ingame_service.model.PartidaJugador;
import com.arimar.gwent.ingame_service.repository.PartidaJugadorRepository;

import com.arimar.gwent.ingame_service.service.PartidaJugadorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartidaJugadorServiceImpl implements PartidaJugadorService {
    private final PartidaJugadorRepository partidaJugadorRepository;
    public PartidaJugadorServiceImpl(PartidaJugadorRepository partidaJugadorRepository){
        this.partidaJugadorRepository=partidaJugadorRepository;
    }
    @Override
    public PartidaJugador crearPartidaJugador(Partida partida, Jugador jugador, boolean turno) {
        PartidaJugador partidaJugador = new PartidaJugador();
        partidaJugador.setPartida(partida);
        partidaJugador.setJugador(jugador);
        partidaJugador.setPaso(false);
        partidaJugador.setTurno(turno);
        partidaJugador.setVida(2);
        partidaJugador.setDescarto(false);
        partidaJugador.setPreparoMazo(false);
        return partidaJugadorRepository.save(partidaJugador);
    }
    @Override
    public PartidaJugador guardarPartidaJugador(PartidaJugador partidaJugador){
        return partidaJugadorRepository.save(partidaJugador);
    }
    @Override
    public List<PartidaJugador> obtenerPartidasJugadorDeUnaPartida(Partida partida){
        return partidaJugadorRepository.traerPartidasJugadorDeUnaPartida(partida.getId());
    }
}
