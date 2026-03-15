package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.IngameServiceClient;
import com.arimar.gwent.bff.dto.ingame.CartaCatalogoDTO;
import com.arimar.gwent.common.response.GenericResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardGatewayController {

    private final IngameServiceClient ingameClient;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaCatalogoDTO>> getCards(
            @RequestParam(required = false) String faccion,
            @RequestParam(required = false) String fila,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean esHeroe,
            @RequestParam(required = false) String habilidad,
            @RequestParam(required = false) Boolean esEspecial) {
        return ingameClient.getCards(faccion, fila, tipo, esHeroe, habilidad, esEspecial);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<CartaCatalogoDTO> getCardById(@PathVariable Long id) {
        return ingameClient.getCardById(id);
    }
}
