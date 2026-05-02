package com.arimar.gwent.ingameservice.entity;

import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gw_carta_catalogo")
public class CartaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Faccion faccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCarta tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private FilaCarta fila;

    private Integer fuerza;

    private Integer fuerzaTransformada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabilidadCarta habilidad;

    @Column(nullable = false)
    private boolean esHeroe;

    @Column(nullable = false, columnDefinition = "int not null default 1")
    private int maxCopias;

    private String imagenUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}
