package com.arimar.gwent.ingame_service.exception.bussines;

public class CartaNoSeEncuentraEnElMazoException extends Exception{
    public CartaNoSeEncuentraEnElMazoException(){
        super("La carta a tirar no se encuentra en el mazo");
    }
}
