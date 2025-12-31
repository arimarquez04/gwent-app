package com.arimar.gwent.ingame_service.exception.bussines;

public class DatoDeJugadorInvalidoException extends Exception{
    public DatoDeJugadorInvalidoException(){
        super("JugadorRequestDto recibio un dato invalido");
    }
}
