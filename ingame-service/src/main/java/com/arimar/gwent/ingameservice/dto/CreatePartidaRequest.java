package com.arimar.gwent.ingameservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePartidaRequest {

    @NotNull
    private UUID oponenteId;

    @NotNull
    private Long mazoId;

    @NotNull
    private Long mazoOponenteId;
}
