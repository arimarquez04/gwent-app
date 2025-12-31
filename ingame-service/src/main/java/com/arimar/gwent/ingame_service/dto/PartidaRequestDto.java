package com.arimar.gwent.ingame_service.dto;

import com.arimar.gwent.ingame_service.model.EstadoPartida;
import com.arimar.gwent.ingame_service.model.Partida;

@Deprecated
public class PartidaRequestDto {
    private EstadoPartida estadoPartida;
    private int ronda;
    private int ganadorId;
    private int perdedorId;

    public EstadoPartida getEstadoPartida() {
        return estadoPartida;
    }

    public void setEstadoPartida(EstadoPartida estadoPartida) {
        this.estadoPartida = estadoPartida;
    }

    public int getRonda() {
        return ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = ronda;
    }

    public int getGanadorId() {
        return ganadorId;
    }

    public void setGanadorId(int ganadorId) {
        this.ganadorId = ganadorId;
    }

    public int getPerdedorId() {
        return perdedorId;
    }

    public void setPerdedorId(int perdedorId) {
        this.perdedorId = perdedorId;
    }
    public Partida convert(){
        Partida partida = new Partida();
        partida.setEstadoPartida(estadoPartida);
        partida.setRonda(ronda);
        return partida;
    }
}
