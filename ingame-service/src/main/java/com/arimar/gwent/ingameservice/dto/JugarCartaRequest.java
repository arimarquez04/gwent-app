package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JugarCartaRequest {

    @NotNull
    private Long cartaPartidaId;

    private FilaCarta fila;
}
