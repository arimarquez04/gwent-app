package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

@Data
public class CartaCatalogoDTO {
    private Long id;
    private String nombre;
    private String faccion;
    private String tipo;
    private String fila;
    private Integer fuerza;
    private String habilidad;
    private boolean esHeroe;
    private String imagenUrl;
}
