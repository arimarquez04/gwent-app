package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.exception.BadRequestException;
import com.arimar.gwent.ingame_service.dto.CartaRequestDto;
import com.arimar.gwent.ingame_service.model.Carta;
import com.arimar.gwent.ingame_service.model.Jugador;

import java.util.List;
import java.util.Optional;

@Deprecated
public interface CartaService {
    public List<Carta> traerTodasLasCartas();
    List<Carta>traerTodasLasCartasDeUnJugador(Jugador jugador);

    public Carta guardarCarta(CartaRequestDto carta, Jugador jugador) throws BadRequestException.JugadorNoExisteException;
    public void eliminarCarta(Carta carta);
    public Optional<Carta> traerCarta(Carta carta);
    public Optional<Carta> traerCartaPorId(int id);
}
