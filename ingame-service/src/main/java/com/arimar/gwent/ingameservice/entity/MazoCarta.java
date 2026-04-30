package com.arimar.gwent.ingameservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "gw_mazo_carta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mazo_id", "carta_catalogo_id"}))
public class MazoCarta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mazo_id", nullable = false)
    private Mazo mazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carta_catalogo_id", nullable = false)
    private CartaCatalogo cartaCatalogo;

    @Column(nullable = false)
    private int cantidad;
}
