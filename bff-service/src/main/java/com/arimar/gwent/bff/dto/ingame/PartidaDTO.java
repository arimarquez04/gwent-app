package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PartidaDTO {

    private Long id;
    private String estado;
    private int rondaActual;
    private boolean esMiTurno;

    private JugadorPartidaDTO yo;
    private JugadorPartidaDTO oponente;

    private List<RondaDTO> rondas;
    private List<CartaPartidaDTO> climaEnCampo;
    private UUID ganadorId;
    private boolean empate;

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
