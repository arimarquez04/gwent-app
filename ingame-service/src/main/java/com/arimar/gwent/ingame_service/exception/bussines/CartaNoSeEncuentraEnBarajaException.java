package com.arimar.gwent.ingame_service.exception.bussines;

import com.arimar.gwent.ingame_service.model.UbicacionCarta;

public class CartaNoSeEncuentraEnBarajaException extends Exception{
    public CartaNoSeEncuentraEnBarajaException(int id, UbicacionCarta ubicacionCarta){
        super("La carta con id" + id + " a tirar no se encuentra en la BARAJA, ubicacion carta: " + ubicacionCarta);
    }
}
