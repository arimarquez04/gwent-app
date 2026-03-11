package com.arimar.gwent.jugadorservice.domain.player;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gw_jugador")
public class JugadorEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name="apodo", unique = true, nullable = false)
    private String apodo;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "nivel")
    private Integer nivel;

    @Column(name = "victories")
    private Integer victorias;

    @Column(name = "defeats")
    private Integer derrotas;

    @Column(name="ties")
    private Integer empates;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
