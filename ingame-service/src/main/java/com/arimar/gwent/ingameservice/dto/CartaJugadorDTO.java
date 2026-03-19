package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.entity.CartaJugador;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CartaJugadorDTO {

    private Long id;
    private int cantidad;
    private LocalDateTime unlockedAt;
    private CartaCatalogoDTO carta;

    public static CartaJugadorDTO from(CartaJugador cartaJugador) {
        return CartaJugadorDTO.builder()
                .id(cartaJugador.getId())
                .cantidad(cartaJugador.getCantidad())
                .unlockedAt(cartaJugador.getUnlockedAt())
                .carta(CartaCatalogoDTO.from(cartaJugador.getCartaCatalogo()))
                .build();
    }
}
