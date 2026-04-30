package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MazoDTO {
    private Long id;
    private String nombre;
    private String faccion;
    private String estado;
    private CartaCatalogoDTO lider;
    private List<MazoCartaDTO> cartas;
    private int totalUnidades;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
