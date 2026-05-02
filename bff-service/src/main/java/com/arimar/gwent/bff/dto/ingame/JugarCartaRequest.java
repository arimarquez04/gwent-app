package com.arimar.gwent.bff.dto.ingame;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JugarCartaRequest {

    @NotNull
    private Long cartaPartidaId;

    private String fila;

    // MEDICO: optional card to revive from graveyard
    private Long reviveCartaId;
    private String reviveFila; // only needed if the revived card has fila=AGIL

    // DECOY: required target card on your field to return to hand
    private Long targetCartaId;
}
