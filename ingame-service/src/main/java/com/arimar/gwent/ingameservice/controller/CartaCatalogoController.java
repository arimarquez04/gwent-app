package com.arimar.gwent.ingameservice.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.ingameservice.domain.Faccion;
import com.arimar.gwent.ingameservice.domain.FilaCarta;
import com.arimar.gwent.ingameservice.domain.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.TipoCarta;
import com.arimar.gwent.ingameservice.dto.CartaCatalogoDTO;
import com.arimar.gwent.ingameservice.service.CartaCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingame/v1/cards")
@RequiredArgsConstructor
public class CartaCatalogoController {

    private final CartaCatalogoService service;

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaCatalogoDTO>> getAll(
            @RequestParam(required = false) Faccion faccion,
            @RequestParam(required = false) FilaCarta fila,
            @RequestParam(required = false) TipoCarta tipo,
            @RequestParam(required = false) Boolean esHeroe,
            @RequestParam(required = false) HabilidadCarta habilidad,
            @RequestParam(required = false) Boolean esEspecial) {
        return GenericResponseDTO.<List<CartaCatalogoDTO>>builder()
                .serviceOrigin(serviceName)
                .data(service.getAll(faccion, fila, tipo, esHeroe, habilidad, esEspecial))
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<CartaCatalogoDTO> getById(@PathVariable Long id) {
        return GenericResponseDTO.<CartaCatalogoDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.getById(id))
                .status(HttpStatus.OK.value())
                .build();
    }
}
