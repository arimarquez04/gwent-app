package com.arimar.gwent.ingameservice.entity;

import com.arimar.gwent.ingameservice.domain.enums.EstadoPartida;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "gw_partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jugador_uno_id", nullable = false)
    private UUID jugadorUnoId;

    @Column(name = "jugador_dos_id", nullable = false)
    private UUID jugadorDosId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mazo_jugador_uno_id", nullable = false)
    private Mazo mazoJugadorUno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mazo_jugador_dos_id", nullable = false)
    private Mazo mazoJugadorDos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPartida estado;

    @Column(nullable = false)
    private int rondaActual;

    @Column(name = "turno_jugador_id")
    private UUID turnoJugadorId;

    @Column(nullable = false)
    private int vidasJugadorUno;

    @Column(nullable = false)
    private int vidasJugadorDos;

    @Column(nullable = false)
    private boolean jugadorUnoPaso;

    @Column(nullable = false)
    private boolean jugadorDosPaso;

    @Column(nullable = false)
    private boolean jugadorUnoMulligan;

    @Column(nullable = false)
    private boolean jugadorDosMulligan;

    @Column(name = "ganador_id")
    private UUID ganadorId;

    @Column(nullable = false)
    private boolean empate;

    @Column(nullable = false, columnDefinition = "tinyint(1) not null default 0")
    private boolean liderUsadoJugadorUno;

    @Column(nullable = false, columnDefinition = "tinyint(1) not null default 0")
    private boolean liderUsadoJugadorDos;

    @Column(name = "cartas_reveladas_j1", length = 200)
    private String cartasReveladasJ1;

    @Column(name = "cartas_reveladas_j2", length = 200)
    private String cartasReveladasJ2;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL)
    private List<CartaPartida> cartas = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL)
    private List<Ronda> rondas = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
