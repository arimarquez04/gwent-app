package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.EstadoMazo;
import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import com.arimar.gwent.ingameservice.entity.Mazo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MazoDTO {

    private Long id;
    private String nombre;
    private Faccion faccion;
    private EstadoMazo estado;
    private CartaCatalogoDTO lider;
    private List<MazoCartaDTO> cartas;
    private int totalUnidades;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static MazoDTO from(Mazo mazo) {
        List<MazoCartaDTO> cartasDTO = mazo.getCartas().stream()
                .map(MazoCartaDTO::from)
                .toList();

        int totalUnidades = mazo.getCartas().stream()
                .filter(mc -> mc.getCartaCatalogo().getTipo() == TipoCarta.UNIDAD)
                .mapToInt(mc -> mc.getCantidad())
                .sum();

        return MazoDTO.builder()
                .id(mazo.getId())
                .nombre(mazo.getNombre())
                .faccion(mazo.getFaccion())
                .estado(mazo.getEstado())
                .lider(mazo.getLider() != null ? CartaCatalogoDTO.from(mazo.getLider()) : null)
                .cartas(cartasDTO)
                .totalUnidades(totalUnidades)
                .createdAt(mazo.getCreatedAt())
                .modifiedAt(mazo.getModifiedAt())
                .build();
    }
}
