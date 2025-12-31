package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CartaPartidaJugadorService {


    void saveCartasPartidaJugador(List<CartaPartidaJugador> cartaPartidaJugadores);

    public CartaPartidaJugador crearCartaPartidaJugador(PartidaJugador partidaJugador, Carta carta, UbicacionCarta ubicacionCarta);
    public void guardarCartaPartidaJugador(CartaPartidaJugador cartaPartidaJugador);
    public void guardarCartaPartidaJugador(List<CartaPartidaJugador> cartaPartidaJugador);
    List<CartaPartidaJugador> traerTodasLasCartaPartidaJugadorDeUnaPartidaJugador(PartidaJugador partidaJugador);

    CartaPartidaJugador traerCartaPartidaJugadorDeUnaCarta(Carta carta, PartidaJugador partidaJugador);

    List<CartaPartidaJugador> traerCPJDeUnaPartidaJugadorEnUnaUbicacionDeterminada
            (PartidaJugador partidaJugador, UbicacionCarta ubicacionCarta);
}
