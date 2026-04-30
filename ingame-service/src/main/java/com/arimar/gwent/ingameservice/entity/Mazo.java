package com.arimar.gwent.ingameservice.entity;

import com.arimar.gwent.ingameservice.domain.enums.EstadoMazo;
import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "gw_mazo")
public class Mazo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jugador_id", nullable = false)
    private UUID jugadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Faccion faccion;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoMazo estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lider_id")
    private CartaCatalogo lider;

    @OneToMany(mappedBy = "mazo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MazoCarta> cartas = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}
