package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CartaCatalogoRepository extends JpaRepository<CartaCatalogo, Long>,
        JpaSpecificationExecutor<CartaCatalogo> {
}
