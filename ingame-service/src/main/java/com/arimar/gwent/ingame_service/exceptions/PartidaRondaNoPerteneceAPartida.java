package com.arimar.gwent.ingame_service.exceptions;

public class PartidaRondaNoPerteneceAPartida extends Exception{
    public PartidaRondaNoPerteneceAPartida(int partidaRondaId, int partidaID, int partidaIDALaQuePertenece){
        super("La PartidaRonda con id: " + partidaRondaId + " pertenece a la partida con id:" + partidaIDALaQuePertenece
                + " y no a la partida con id: " + partidaID);
    }
}
