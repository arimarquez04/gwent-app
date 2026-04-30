package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.Faccion;
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
    private Faccion faccion;

    private Long liderId;

    private List<MazoCartaEntrada> cardEntries = new ArrayList<>();
}
