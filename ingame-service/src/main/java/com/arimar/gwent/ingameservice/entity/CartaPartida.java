package com.arimar.gwent.ingameservice.entity;

import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.ZonaCarta;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "gw_carta_partida")
public class CartaPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(name = "jugador_id", nullable = false)
    private UUID jugadorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carta_catalogo_id", nullable = false)
    private CartaCatalogo cartaCatalogo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ZonaCarta zona;

    @Enumerated(EnumType.STRING)
    @Column(name = "fila_en_campo", length = 20)
    private FilaCarta filaEnCampo;
}
