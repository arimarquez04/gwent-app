package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.model.Jugador;
import com.arimar.gwent.ingame_service.model.Partida;
import com.arimar.gwent.ingame_service.model.PartidaJugador;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Deprecated
public interface PartidaJugadorService {


    PartidaJugador crearPartidaJugador(Partida partida, Jugador jugador, boolean turno);

    PartidaJugador guardarPartidaJugador(PartidaJugador partidaJugador);

    List<PartidaJugador> obtenerPartidasJugadorDeUnaPartida(Partida partida);
}
