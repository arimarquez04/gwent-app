package com.arimar.gwent.ingame_service.exceptions;

public class JugadorNoJuegaPorquePasoException extends Exception{
    public JugadorNoJuegaPorquePasoException(int id){
        super("El jugador con id: " + id + " no puede jugar porque ya pasó su turno");
    }
}
