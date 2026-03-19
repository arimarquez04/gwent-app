package com.arimar.gwent.ingameservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "gw_carta_jugador", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"jugador_id", "carta_catalogo_id"})
})
public class CartaJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jugador_id", nullable = false)
    private UUID jugadorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carta_catalogo_id", nullable = false)
    private CartaCatalogo cartaCatalogo;

    @Column(nullable = false, columnDefinition = "int not null default 1")
    private int cantidad;

    @Column(nullable = false, updatable = false)
    private LocalDateTime unlockedAt;

    @PrePersist
    void onCreate() {
        this.unlockedAt = LocalDateTime.now();
    }
}
