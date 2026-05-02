package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import com.arimar.gwent.ingameservice.entity.CartaPartida;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartaPartidaDTO {

    private Long id;
    private Long cartaCatalogoId;
    private String nombre;
    private Faccion faccion;
    private TipoCarta tipo;
    private FilaCarta fila;
    private Integer fuerza;
    private Integer fuerzaTransformada;
    private HabilidadCarta habilidad;
    private boolean esHeroe;
    private boolean transformado;
    private boolean esSlotLateral;
    private String imagenUrl;

    public static CartaPartidaDTO from(CartaPartida cp) {
        var carta = cp.getCartaCatalogo();
        return CartaPartidaDTO.builder()
                .id(cp.getId())
                .cartaCatalogoId(carta.getId())
                .nombre(carta.getNombre())
                .faccion(carta.getFaccion())
                .tipo(carta.getTipo())
                .fila(carta.getFila())
                .fuerza(carta.getFuerza())
                .fuerzaTransformada(carta.getFuerzaTransformada())
                .habilidad(carta.getHabilidad())
                .esHeroe(carta.isEsHeroe())
                .transformado(cp.isTransformado())
                .esSlotLateral(cp.isEsSlotLateral())
                .imagenUrl(carta.getImagenUrl())
                .build();
    }
}
