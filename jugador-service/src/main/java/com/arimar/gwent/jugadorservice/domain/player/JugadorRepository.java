package com.arimar.gwent.jugadorservice.domain.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JugadorRepository extends JpaRepository<JugadorEntity, UUID> {
}
