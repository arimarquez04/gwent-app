package com.arimar.gwent.ingame_service.exceptions;

public class JugadorYaPreparoMazoException extends Exception{
    public JugadorYaPreparoMazoException(int id, boolean preparoMazo){
        super("El jugador con id: " + id + " ya preparo mazo: preparo_mazo: " + preparoMazo);
    }
}
