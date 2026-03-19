package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartaJugadorDTO {
    private Long id;
    private int cantidad;
    private LocalDateTime unlockedAt;
    private CartaCatalogoDTO carta;
}
