package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartaCatalogoDTO {

    private Long id;
    private String nombre;
    private Faccion faccion;
    private TipoCarta tipo;
    private FilaCarta fila;
    private Integer fuerza;
    private HabilidadCarta habilidad;
    private boolean esHeroe;
    private int maxCopias;
    private String imagenUrl;

    public static CartaCatalogoDTO from(CartaCatalogo carta) {
        return CartaCatalogoDTO.builder()
                .id(carta.getId())
                .nombre(carta.getNombre())
                .faccion(carta.getFaccion())
                .tipo(carta.getTipo())
                .fila(carta.getFila())
                .fuerza(carta.getFuerza())
                .habilidad(carta.getHabilidad())
                .esHeroe(carta.isEsHeroe())
                .maxCopias(carta.getMaxCopias())
                .imagenUrl(carta.getImagenUrl())
                .build();
    }
}
