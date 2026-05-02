package com.arimar.gwent.jugadorservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class MatchResultRequest {
    private UUID jugadorUnoId;
    private UUID jugadorDosId;
    private UUID ganadorId;   // null when empate=true
    private boolean empate;
}
