package com.arimar.gwent.ingame_service.exceptions;

public class JugadorConApodoDuplicadoException extends Exception{
    public JugadorConApodoDuplicadoException(String apodo){
        super("Apodo:" + apodo + " duplicado.");
    }
}
