package com.arimar.gwent.ingame_service.exceptions;

public class CartaAJugarYaSeEncuentraJugadaException extends Exception{
    public CartaAJugarYaSeEncuentraJugadaException(int id){
        super("La carta con id: " + id + " ya se encuentra jugada");
    }
}
