package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.model.Partida;
import com.arimar.gwent.ingame_service.model.PartidaJugador;
import com.arimar.gwent.ingame_service.model.PartidaRonda;
import org.springframework.stereotype.Service;

@Service
@Deprecated
public interface PartidaRondaService {
    PartidaRonda crearPartidaRonda(Partida partida, int numeroRonda, PartidaJugador partidaJugadorUno, PartidaJugador partidaJugadorDos);

    PartidaRonda guardarPartidaRonda(PartidaRonda partidaRonda);

    PartidaRonda traerPartidaRondaDeUnaPartida(Partida partida, int ronda);
}
