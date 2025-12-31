package com.arimar.gwent.ingame_service.exceptions;

public class DatoDeJugadorInvalidoException extends Exception{
    public DatoDeJugadorInvalidoException(){
        super("JugadorRequestDto recibio un dato invalido");
    }
}
