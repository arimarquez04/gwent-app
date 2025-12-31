package com.arimar.gwent.ingame_service.exceptions;

import com.arimar.gwent.ingame_service.model.EstadoPartida;

public class PartidaNoEstaEnDescarteException extends Exception{
    public PartidaNoEstaEnDescarteException(int partidaId, EstadoPartida estadoPartida){
        super("La partida con id: " + partidaId + " no se encuentra en DESCARTE, estado partida: " + estadoPartida);
    }
}
