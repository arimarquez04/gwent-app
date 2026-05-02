package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

@Data
public class CartaPartidaDTO {

    private Long id;
    private Long cartaCatalogoId;
    private String nombre;
    private String faccion;
    private String tipo;
    private String fila;
    private Integer fuerza;
    private Integer fuerzaTransformada;
    private String habilidad;
    private boolean esHeroe;
    private boolean transformado;
    private boolean esSlotLateral;
    private String imagenUrl;
}
