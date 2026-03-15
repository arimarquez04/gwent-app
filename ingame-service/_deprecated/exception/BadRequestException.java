package com.arimar.gwent.ingame_service.exception;

import com.arimar.gwent.ingame_service.model.EstadoPartida;
import org.springframework.validation.BindingResult;

public class BadRequestException extends Exception {
    private final transient BindingResult bindingResult;

    public BadRequestException(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
    }

    public BindingResult getBindingResult() {
        return bindingResult;
    }

    public static class JugadorAunNoPreparoMazo extends Exception{
        public JugadorAunNoPreparoMazo(int id){

        }
    }

    public static class JugadorConApodoDuplicadoException extends Exception{
        public JugadorConApodoDuplicadoException(String apodo){
            super("Apodo:" + apodo + " duplicado.");
        }
    }

    public static class JugadorNoExisteException extends Exception {
        public JugadorNoExisteException() {
            super("No existe el jugador solicitado");
        }
        public JugadorNoExisteException(int id) {
            super("No existe el jugador solicitado con id: " + id);
        }
    }

    public static class JugadorNoJuegaPorquePasoException extends Exception{
        public JugadorNoJuegaPorquePasoException(int id){
            super("El jugador con id: " + id + " no puede jugar porque ya pasó su turno");
        }
    }

    public static class JugadorNoLeTocaJugarException extends Exception{
        public JugadorNoLeTocaJugarException(){
            super("No es turno del jugador");
        }
        public JugadorNoLeTocaJugarException(int jugadorId){
            super("No es turno del jugador con id: " + jugadorId);}
    }

    public static class JugadorNoPerteneceAPartidaException extends Exception{
        public JugadorNoPerteneceAPartidaException(int partidaId, int jugadorId){
            super("El jugador con id " + jugadorId + " no pertenece a la partida con id " + partidaId);
        }
    }

    public static class JugadorYaDescartoException extends Exception{
        public JugadorYaDescartoException(int id, boolean descarto){
            super("El jugador con id: " + id + " ya descarto isDescarto()= " + descarto);
        }
    }

    public static class JugadorYaPreparoMazoException extends Exception{
        public JugadorYaPreparoMazoException(int id, boolean preparoMazo){
            super("El jugador con id: " + id + " ya preparo mazo: preparo_mazo: " + preparoMazo);
        }
    }

    public static class NoSePuedeBorrarJugadorConCartasException extends Exception{
        public NoSePuedeBorrarJugadorConCartasException() {
            super("El jugador que se intenta borrar tiene cartas asignadas");
        }
        public NoSePuedeBorrarJugadorConCartasException(int id) {
            super("El jugador con id: " + id + " no se puede borrar porque tiene cartas asignadas");
        }
    }

    public static class NoSePuedeEnviarCartasAlCementerioPorqueLosJugadoresNoPasaronException extends Exception {
        public NoSePuedeEnviarCartasAlCementerioPorqueLosJugadoresNoPasaronException(int idPartida){
            super("No se pueden enviar las cartas al cementerio de la partida con id: " + idPartida + " porque los jugadores no pasaron");
        }
    }

    public static class NoSePuedeEnviarCartasAlCementerioPorqueUnJugadorNoPasoException extends Exception {
        public NoSePuedeEnviarCartasAlCementerioPorqueUnJugadorNoPasoException(int idPartida, int idJugador){
            super("No se pueden enviar las cartas al cementerio de la partida con id: " + idPartida +
                    " porque el jugador con id: " + idJugador + " no pasó");
        }
    }

    public static class PartidaJugadorNoPerteneceAPartidaRondaException extends Exception {

    }

    public static class PartidaNoEstaEnCursoException extends Exception{
        public PartidaNoEstaEnCursoException(int partidaId, EstadoPartida estadoPartida){
            super("La partida con id: " + partidaId + " no se encuentra EN_CURSO, estado partida: " + estadoPartida);
        }
    }

    public static class PartidaNoEstaEnDescarteException extends Exception{
        public PartidaNoEstaEnDescarteException(int partidaId, EstadoPartida estadoPartida){
            super("La partida con id: " + partidaId + " no se encuentra en DESCARTE, estado partida: " + estadoPartida);
        }
    }

    public static class PartidaNoEstaEnPreparandoMazoException extends Exception{
        public PartidaNoEstaEnPreparandoMazoException(int partidaId, EstadoPartida estadoPartida){
            super("La partida con id: " + partidaId + " no se encuentra en PREPARANDO_MAZO, estado partida: " + estadoPartida);
        }
    }

    public static class PartidaNoExisteException extends Exception {
        public PartidaNoExisteException() {
            super("No existe la partida solicitada");
        }
        public PartidaNoExisteException(int id) {
            super("No existe la partida solicitada con id: " + id);
        }
    }

    public static class PartidaRondaNoCoincideConRondaDePartidaException extends Exception{
        public PartidaRondaNoCoincideConRondaDePartidaException(){
            super("La ronda no coincide con el numero de ronda de la partida");
        }

    }

    public static class PartidaRondaNoPerteneceAPartida extends Exception{
        public PartidaRondaNoPerteneceAPartida(int partidaRondaId, int partidaID, int partidaIDALaQuePertenece){
            super("La PartidaRonda con id: " + partidaRondaId + " pertenece a la partida con id:" + partidaIDALaQuePertenece
                    + " y no a la partida con id: " + partidaID);
        }
    }
}
