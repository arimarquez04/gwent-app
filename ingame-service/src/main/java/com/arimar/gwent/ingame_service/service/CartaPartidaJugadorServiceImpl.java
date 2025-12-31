package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.model.Carta;
import com.arimar.gwent.ingame_service.model.CartaPartidaJugador;
import com.arimar.gwent.ingame_service.model.PartidaJugador;
import com.arimar.gwent.ingame_service.model.UbicacionCarta;
import com.arimar.gwent.ingame_service.repository.CartaPartidaJugadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartaPartidaJugadorServiceImpl implements CartaPartidaJugadorService {
    private final CartaPartidaJugadorRepository cartaPartidaJugadorRepository;

    public CartaPartidaJugadorServiceImpl(CartaPartidaJugadorRepository partidaJugadorRepository) {
        this.cartaPartidaJugadorRepository = partidaJugadorRepository;
    }

    @Override
    public void saveCartasPartidaJugador(List<CartaPartidaJugador> cartaPartidaJugadores) {
        for (CartaPartidaJugador c : cartaPartidaJugadores) {
            cartaPartidaJugadorRepository.save(c);
        }
    }

    @Override
    public CartaPartidaJugador crearCartaPartidaJugador(PartidaJugador partidaJugador, Carta carta, UbicacionCarta ubicacionCarta) {
        CartaPartidaJugador cartaPartidaJugador = new CartaPartidaJugador();
        cartaPartidaJugador.setPartidaJugador(partidaJugador);
        cartaPartidaJugador.setCarta(carta);
        cartaPartidaJugador.setUbicacionCarta(ubicacionCarta);
        cartaPartidaJugadorRepository.save(cartaPartidaJugador);
        return cartaPartidaJugador;
    }

    @Override
    public void guardarCartaPartidaJugador(CartaPartidaJugador cartaPartidaJugador) {
        cartaPartidaJugadorRepository.save(cartaPartidaJugador);
    }

    @Override
    public void guardarCartaPartidaJugador(List<CartaPartidaJugador> cartaPartidaJugadores) {
        for (CartaPartidaJugador c : cartaPartidaJugadores){
            cartaPartidaJugadorRepository.save(c);
        }
    }

    @Override
    public List<CartaPartidaJugador> traerTodasLasCartaPartidaJugadorDeUnaPartidaJugador(PartidaJugador partidaJugador) {
        return cartaPartidaJugadorRepository.traerCartaPartidaJugadorDeUnaPartidaJugador(partidaJugador.getId());
    }
    @Override
    public CartaPartidaJugador traerCartaPartidaJugadorDeUnaCarta(Carta carta, PartidaJugador partidaJugador) {
        return cartaPartidaJugadorRepository.traerCartaPartidaJugadorDeUnaCarta(carta.getId(), partidaJugador.getId());
    }
    @Override
    public List<CartaPartidaJugador> traerCPJDeUnaPartidaJugadorEnUnaUbicacionDeterminada
            (PartidaJugador partidaJugador, UbicacionCarta ubicacionCarta){
        return cartaPartidaJugadorRepository.
                traerCartasPartidaJugadorEnUbicacionDeterminada(partidaJugador.getId(), ubicacionCarta.toString());
    }
}
