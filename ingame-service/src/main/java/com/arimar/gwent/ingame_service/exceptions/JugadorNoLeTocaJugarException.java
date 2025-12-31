package com.arimar.gwent.ingame_service.exceptions;

public class JugadorNoLeTocaJugarException extends Exception{
    public JugadorNoLeTocaJugarException(){
        super("No es turno del jugador");
    }
    public JugadorNoLeTocaJugarException(int jugadorId){
        super("No es turno del jugador con id: " + jugadorId);}
}
