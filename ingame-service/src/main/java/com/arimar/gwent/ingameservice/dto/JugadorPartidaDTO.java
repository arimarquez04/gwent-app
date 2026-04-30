package com.arimar.gwent.ingameservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JugadorPartidaDTO {

    private UUID jugadorId;
    private int vidas;
    private boolean paso;
    private boolean mulliganCompleto;

    private List<CartaPartidaDTO> mano;
    private int manoCount;
    private int mazoCount;
    private TableroDTO tablero;
    private List<CartaPartidaDTO> cementerio;
    private int fuerzaTotal;
}
