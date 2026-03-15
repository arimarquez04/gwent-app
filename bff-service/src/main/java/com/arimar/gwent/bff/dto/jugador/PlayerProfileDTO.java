package com.arimar.gwent.bff.dto.jugador;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PlayerProfileDTO {
    private UUID userId;
    private String apodo;
    private String avatarUrl;
    private int nivel;
    private int victorias;
    private int derrotas;
    private int empates;
    private LocalDateTime createdAt;
}
