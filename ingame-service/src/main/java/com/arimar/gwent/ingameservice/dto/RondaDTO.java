package com.arimar.gwent.ingameservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RondaDTO {

    private int numeroRonda;
    private int miPuntaje;
    private int puntajeOponente;
    private UUID ganadorId;
    private boolean empate;
}
