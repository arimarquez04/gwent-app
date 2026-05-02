package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.IngameServiceClient;
import com.arimar.gwent.bff.dto.ingame.CreatePartidaRequest;
import com.arimar.gwent.bff.dto.ingame.JugarCartaRequest;
import com.arimar.gwent.bff.dto.ingame.MulliganRequest;
import com.arimar.gwent.bff.dto.ingame.PartidaDTO;
import com.arimar.gwent.bff.dto.ingame.UsarLiderRequest;
import com.arimar.gwent.common.response.GenericResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partidas")
@RequiredArgsConstructor
public class PartidaGatewayController {

    private final IngameServiceClient ingameClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<PartidaDTO> createPartida(@Valid @RequestBody CreatePartidaRequest request) {
        return ingameClient.createPartida(request);
    }

    @GetMapping("/{id}")
    public GenericResponseDTO<PartidaDTO> getPartida(@PathVariable Long id) {
        return ingameClient.getPartida(id);
    }

    @PostMapping("/{id}/mulligan")
    public GenericResponseDTO<PartidaDTO> mulligan(@PathVariable Long id,
                                                    @RequestBody MulliganRequest request) {
        return ingameClient.mulligan(id, request);
    }

    @PostMapping("/{id}/jugar-carta")
    public GenericResponseDTO<PartidaDTO> jugarCarta(@PathVariable Long id,
                                                      @RequestBody JugarCartaRequest request) {
        return ingameClient.jugarCarta(id, request);
    }

    @PostMapping("/{id}/pasar")
    public GenericResponseDTO<PartidaDTO> pasar(@PathVariable Long id) {
        return ingameClient.pasar(id);
    }

    @PostMapping("/{id}/usar-lider")
    public GenericResponseDTO<PartidaDTO> usarLider(@PathVariable Long id,
                                                     @RequestBody(required = false) UsarLiderRequest request) {
        return ingameClient.usarLider(id, request);
    }
}
