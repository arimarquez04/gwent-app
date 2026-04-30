package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.List;

@Data
public class UpdateMazoRequest {

    private String nombre;

    // null → no cambia el lider; -1 → quita el lider
    private Long liderId;

    // null → no cambia las cartas
    private List<MazoCartaEntrada> cardEntries;
}
