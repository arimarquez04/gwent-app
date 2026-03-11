package com.arimar.gwent.jugadorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
