package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.ingame_service.exception.*;
import com.arimar.gwent.ingame_service.exception.bussines.*;
import com.arimar.gwent.ingame_service.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
@Deprecated
public interface PartidaService {
    public List<Partida> traerTodasLasPartida();

    public Optional<Partida> traerPartidaPorId(int id);

    List<PartidaJugador> obtenerPartidasJugadorDeUnaPartida(Partida partida);



    List<CartaPartidaJugador> obtenerCartasPartidaJugadorDeUnJugadorEnPartida(Partida partida, Jugador jugador)
            throws BadRequestException.JugadorNoPerteneceAPartidaException;

    List<Carta> obtenerCartasEnMazoDeUnJugadorEnPartida(Partida partida, Jugador jugador) throws BadRequestException.JugadorNoPerteneceAPartidaException;

    List<Carta> obtenerCartasEnBarajaDeUnJugadorEnPartida(Partida partida, Jugador jugador)
            throws BadRequestException.JugadorNoPerteneceAPartidaException;

    List<Carta> obtenerCartasEnCementerioDeUnJugadorEnPartida(Partida partida, Jugador jugador)
            throws BadRequestException.JugadorNoPerteneceAPartidaException;

    public Partida crearPartida(Jugador jugadorUno, Jugador jugadorDos);

    List<Carta> tirarCarta(Partida partida, Carta carta, Jugador jugador)
            throws CartaNoPerteneceAJugadorException, CartaNoSeEncuentraEnElMazoException, BadRequestException.JugadorNoLeTocaJugarException,
            BadRequestException.JugadorNoJuegaPorquePasoException, BadRequestException.JugadorNoPerteneceAPartidaException, BadRequestException.PartidaNoEstaEnCursoException, CartaNoSeEncuentraEnBarajaException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueUnJugadorNoPasoException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueLosJugadoresNoPasaronException, BadRequestException.PartidaRondaNoPerteneceAPartida, BadRequestException.PartidaRondaNoCoincideConRondaDePartidaException;

    Partida verificarEmpezarPartida(Partida partida,Jugador jugador);

    public List<Carta> prepararMazo(Partida partida, Jugador jugador, List<Carta> cartas)
            throws CartasInsufucientesException, CartaNoPerteneceAJugadorException, BadRequestException.PartidaNoEstaEnPreparandoMazoException, BadRequestException.JugadorYaPreparoMazoException, BadRequestException.JugadorNoPerteneceAPartidaException;

    List<Carta> noDescartarCartas(Partida partida, Jugador jugador) throws BadRequestException.JugadorNoPerteneceAPartidaException, BadRequestException.JugadorNoLeTocaJugarException;

    List<Carta> descarteDosCartas(Partida partida, Jugador jugador, Carta cartaUno, Carta cartaDos)
            throws CartaADescartarNoSeEncuentraEnBarajaException, BadRequestException.JugadorNoPerteneceAPartidaException, CartaNoPerteneceAJugadorException;

    List<Carta> descarteUnaCarta(Partida partida, Jugador jugador, Carta carta)
            throws CartaADescartarNoSeEncuentraEnBarajaException, BadRequestException.JugadorNoPerteneceAPartidaException,
            CartaNoPerteneceAJugadorException;


    Partida jugadorPasaSuTurno(Partida partida, Jugador jugador) throws BadRequestException.JugadorNoPerteneceAPartidaException, BadRequestException.JugadorNoLeTocaJugarException, BadRequestException.PartidaNoEstaEnCursoException, BadRequestException.PartidaRondaNoPerteneceAPartida, BadRequestException.PartidaRondaNoCoincideConRondaDePartidaException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueUnJugadorNoPasoException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueLosJugadoresNoPasaronException;

    boolean jugadorPerteneceAPartida(Jugador jugador, Partida partida);

    PartidaJugador obtenerPartidaJugadorDeUnJugadorEnPartida(Partida partida, Jugador jugador);

    PartidaJugador obtenerPartidaJugadorDelOtroJugadorEnPartida(Partida partida, Jugador jugador);

    List<Carta> descarte(Partida partida, Jugador jugador, Optional<Carta> cartaUnoOptional, Optional<Carta> cartaDosOptional, Boolean noTirar) throws BadRequestException.JugadorNoPerteneceAPartidaException, CartaADescartarNoSeEncuentraEnBarajaException, CartaNoPerteneceAJugadorException, BadRequestException.PartidaNoEstaEnDescarteException, BadRequestException.JugadorNoLeTocaJugarException, BadRequestException.JugadorYaDescartoException;

    Partida enviarCartasAlCementerioYContarPuntajeDeRonda(Partida partida, Jugador jugador) throws BadRequestException.PartidaNoEstaEnCursoException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueLosJugadoresNoPasaronException, BadRequestException.NoSePuedeEnviarCartasAlCementerioPorqueUnJugadorNoPasoException;

    Partida verficarGanador(Partida partida)
            throws BadRequestException.PartidaRondaNoPerteneceAPartida, BadRequestException.PartidaRondaNoCoincideConRondaDePartidaException;
}
