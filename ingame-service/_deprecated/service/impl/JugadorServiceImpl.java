package com.arimar.gwent.ingame_service.service.impl;
import com.arimar.gwent.ingame_service.dto.JugadorRequestDto;
import com.arimar.gwent.ingame_service.exception.BadRequestException;
import com.arimar.gwent.ingame_service.model.Jugador;
import com.arimar.gwent.ingame_service.repository.JugadorRepository;

import com.arimar.gwent.ingame_service.service.CartaService;
import com.arimar.gwent.ingame_service.service.JugadorService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class JugadorServiceImpl implements JugadorService {

    private final JugadorRepository jugadorRepository;
    private final CartaService cartaService;

    public JugadorServiceImpl(JugadorRepository jugadorRepository, CartaService cartaService){
        this.jugadorRepository=jugadorRepository;
        this.cartaService = cartaService;
    }

    @Override
    public List<Jugador> traerTodosLosJugadores() {
        return jugadorRepository.findAll();
    }

    @Override
    public Jugador guardarJugador(JugadorRequestDto jugadorRequestDto) throws BadRequestException.JugadorConApodoDuplicadoException {
        Jugador jugador = jugadorRequestDto.convert();
        Optional<Jugador> jugadorOptional = traerOptionarJugadorPorApodo(jugador.getApodo());
        if (!jugadorOptional.isEmpty()){
            Jugador jugadorDuplicado = jugadorOptional.get();
            throw new BadRequestException.JugadorConApodoDuplicadoException(jugadorDuplicado.getApodo());
        }
        return jugadorRepository.save(jugador);
    }

    @Override
    public void eliminarJugador(Jugador jugador) throws BadRequestException.NoSePuedeBorrarJugadorConCartasException {
        /*List<Carta> cartasDelJugador = cartaService.traerTodasLasCartasDeUnJugador(jugador);
        if (!cartasDelJugador.isEmpty()){
            throw new NoSePuedeBorrarJugadorConCartasException(jugador.getId());
        }*/
        jugadorRepository.deleteById(jugador.getId());
    }

    @Override
    public Optional<Jugador> traerJugador(Jugador jugador) {
        return jugadorRepository.findById(jugador.getId());
    }
    @Override
    public Optional<Jugador> traerOptionarJugadorPorApodo(String apodo){
        return jugadorRepository.traerJugadorPorApodo(apodo);
    }
    @Override
    public Optional<Jugador> traerJugadorPorId(int id) {
        return jugadorRepository.findById(id);
    }

}
