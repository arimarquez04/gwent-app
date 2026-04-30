package com.arimar.gwent.ingameservice.dto;

import com.arimar.gwent.ingameservice.domain.enums.EstadoPartida;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PartidaDTO {

    private Long id;
    private EstadoPartida estado;
    private int rondaActual;
    private boolean esMiTurno;

    private JugadorPartidaDTO yo;
    private JugadorPartidaDTO oponente;

    private List<RondaDTO> rondas;
    private UUID ganadorId;
    private boolean empate;

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
