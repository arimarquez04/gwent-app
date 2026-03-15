package com.arimar.gwent.ingame_service.exception.bussines;

public class CartaADescartarNoSeEncuentraEnBarajaException extends Exception{
    public CartaADescartarNoSeEncuentraEnBarajaException(){
        super("La/s carta/s a descartar no se encuentra/n en la baraja");
    }
}
