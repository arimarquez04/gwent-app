package com.arimar.gwent.ingameservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "gw_ronda")
public class Ronda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(nullable = false)
    private int numeroRonda;

    @Column(nullable = false)
    private int puntajeJugadorUno;

    @Column(nullable = false)
    private int puntajeJugadorDos;

    @Column(name = "ganador_id")
    private UUID ganadorId;

    @Column(nullable = false)
    private boolean empate;
}
