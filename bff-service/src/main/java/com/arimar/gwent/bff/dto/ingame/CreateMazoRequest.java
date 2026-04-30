package com.arimar.gwent.bff.dto.ingame;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateMazoRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private String faccion;

    private Long liderId;

    private List<MazoCartaEntrada> cardEntries = new ArrayList<>();
}
