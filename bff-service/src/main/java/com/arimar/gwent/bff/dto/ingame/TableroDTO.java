package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.List;

@Data
public class TableroDTO {

    private List<CartaPartidaDTO> cuerpoACuerpo;
    private List<CartaPartidaDTO> distancia;
    private List<CartaPartidaDTO> asedio;
    private int fuerzaCuerpoACuerpo;
    private int fuerzaDistancia;
    private int fuerzaAsedio;
    private CartaPartidaDTO slotLateralCuerpoACuerpo;
    private CartaPartidaDTO slotLateralDistancia;
    private CartaPartidaDTO slotLateralAsedio;
}
