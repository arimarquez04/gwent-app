package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

@Data
public class MazoCartaDTO {
    private Long cartaCatalogoId;
    private int cantidad;
    private CartaCatalogoDTO carta;
}
