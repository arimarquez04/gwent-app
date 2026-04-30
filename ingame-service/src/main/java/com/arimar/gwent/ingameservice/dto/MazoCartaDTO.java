package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.entity.MazoCarta;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MazoCartaDTO {

    private Long cartaCatalogoId;
    private int cantidad;
    private CartaCatalogoDTO carta;

    public static MazoCartaDTO from(MazoCarta mazoCarta) {
        return MazoCartaDTO.builder()
                .cartaCatalogoId(mazoCarta.getCartaCatalogo().getId())
                .cantidad(mazoCarta.getCantidad())
                .carta(CartaCatalogoDTO.from(mazoCarta.getCartaCatalogo()))
                .build();
    }
}
