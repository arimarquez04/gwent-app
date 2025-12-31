package com.arimar.gwent.ingame_service.exceptions;

import com.arimar.gwent.ingame_service.model.EstadoPartida;

public class PartidaNoEstaEnCursoException extends Exception{
    public PartidaNoEstaEnCursoException(int partidaId, EstadoPartida estadoPartida){
        super("La partida con id: " + partidaId + " no se encuentra EN_CURSO, estado partida: " + estadoPartida);
    }
}
