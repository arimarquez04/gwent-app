package com.arimar.gwent.ingame_service.exceptions;

import com.arimar.gwent.ingame_service.model.EstadoPartida;

public class PartidaNoEstaEnPreparandoMazoException extends Exception{
    public PartidaNoEstaEnPreparandoMazoException(int partidaId, EstadoPartida estadoPartida){
        super("La partida con id: " + partidaId + " no se encuentra en PREPARANDO_MAZO, estado partida: " + estadoPartida);
    }
}
