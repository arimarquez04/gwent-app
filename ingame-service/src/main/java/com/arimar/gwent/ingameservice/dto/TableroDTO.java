package com.arimar.gwent.ingameservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TableroDTO {

    private List<CartaPartidaDTO> cuerpoACuerpo;
    private List<CartaPartidaDTO> distancia;
    private List<CartaPartidaDTO> asedio;
    private int fuerzaCuerpoACuerpo;
    private int fuerzaDistancia;
    private int fuerzaAsedio;
}
