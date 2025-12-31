package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.dto.JugadorRequestDto;
import com.arimar.gwent.ingame_service.exceptions.JugadorConApodoDuplicadoException;
import com.arimar.gwent.ingame_service.exceptions.NoSePuedeBorrarJugadorConCartasException;
import com.arimar.gwent.ingame_service.model.Jugador;

import java.util.List;
import java.util.Optional;

public interface JugadorService {
    List<Jugador> traerTodosLosJugadores();

    Jugador guardarJugador(JugadorRequestDto jugadorRequestDto) throws JugadorConApodoDuplicadoException;

    void eliminarJugador(Jugador jugador) throws NoSePuedeBorrarJugadorConCartasException;

    Optional<Jugador> traerJugador(Jugador jugador);

    Optional<Jugador> traerOptionarJugadorPorApodo(String apodo);

    Optional<Jugador> traerJugadorPorId(int id);
}
