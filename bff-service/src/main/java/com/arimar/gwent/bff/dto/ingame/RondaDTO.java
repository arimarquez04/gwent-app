package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.UUID;

@Data
public class RondaDTO {

    private int numeroRonda;
    private int miPuntaje;
    private int puntajeOponente;
    private UUID ganadorId;
    private boolean empate;
}
