package com.arimar.gwent.ingame_service.exception.bussines;

public class CartaNoExisteException extends Exception{
    public CartaNoExisteException(){
        super("No existe la carta solicitada");
    }
    public CartaNoExisteException(int id){
        super("No existe la carta solicitada con id: " + id);
    }
}
