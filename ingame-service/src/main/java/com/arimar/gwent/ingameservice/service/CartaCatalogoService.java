package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.Faccion;
import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.enums.TipoCarta;
import com.arimar.gwent.ingameservice.dto.CartaCatalogoDTO;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import com.arimar.gwent.ingameservice.repository.CartaCatalogoRepository;
import com.arimar.gwent.ingameservice.repository.CartaCatalogoSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartaCatalogoService {

    private final CartaCatalogoRepository repo;

    public List<CartaCatalogoDTO> getAll(Faccion faccion, FilaCarta fila, TipoCarta tipo,
                                         Boolean esHeroe, HabilidadCarta habilidad, Boolean esEspecial) {
        Specification<CartaCatalogo> spec = Specification
                .where(CartaCatalogoSpec.withFaccion(faccion))
                .and(CartaCatalogoSpec.withFila(fila))
                .and(Boolean.TRUE.equals(esEspecial)
                        ? CartaCatalogoSpec.esEspecialOClima()
                        : CartaCatalogoSpec.withTipo(tipo))
                .and(CartaCatalogoSpec.withEsHeroe(esHeroe))
                .and(CartaCatalogoSpec.withHabilidad(habilidad));
        return repo.findAll(spec).stream().map(CartaCatalogoDTO::from).toList();
    }

    public CartaCatalogoDTO getById(Long id) {
        return repo.findById(id)
                .map(CartaCatalogoDTO::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }
}
