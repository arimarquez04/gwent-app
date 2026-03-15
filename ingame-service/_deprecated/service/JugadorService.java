package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.dto.JugadorRequestDto;
import com.arimar.gwent.ingame_service.exception.BadRequestException;
import com.arimar.gwent.ingame_service.model.Jugador;

import java.util.List;
import java.util.Optional;
@Deprecated
public interface JugadorService {
    List<Jugador> traerTodosLosJugadores();

    Jugador guardarJugador(JugadorRequestDto jugadorRequestDto) throws BadRequestException.JugadorConApodoDuplicadoException;

    void eliminarJugador(Jugador jugador) throws BadRequestException.NoSePuedeBorrarJugadorConCartasException;

    Optional<Jugador> traerJugador(Jugador jugador);

    Optional<Jugador> traerOptionarJugadorPorApodo(String apodo);

    Optional<Jugador> traerJugadorPorId(int id);
}
