package com.arimar.gwent.ingameservice.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.ingameservice.config.security.ActorResolver;
import com.arimar.gwent.ingameservice.dto.*;
import com.arimar.gwent.ingameservice.service.PartidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingame/v1/partidas")
@RequiredArgsConstructor
public class PartidaController {

    private final PartidaService service;
    private final ActorResolver actorResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<PartidaDTO> createPartida(@Valid @RequestBody CreatePartidaRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.createPartida(actor.getUserId(), request))
                .status(HttpStatus.CREATED.value())
                .build();
    }

    @GetMapping("/{id}")
    public GenericResponseDTO<PartidaDTO> getPartida(@PathVariable Long id) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.getPartida(actor.getUserId(), id))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/{id}/mulligan")
    public GenericResponseDTO<PartidaDTO> mulligan(@PathVariable Long id,
                                                    @RequestBody MulliganRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.mulligan(actor.getUserId(), id, request))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/{id}/jugar-carta")
    public GenericResponseDTO<PartidaDTO> jugarCarta(@PathVariable Long id,
                                                      @Valid @RequestBody JugarCartaRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.jugarCarta(actor.getUserId(), id, request))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/{id}/pasar")
    public GenericResponseDTO<PartidaDTO> pasar(@PathVariable Long id) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.pasar(actor.getUserId(), id))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/{id}/usar-lider")
    public GenericResponseDTO<PartidaDTO> usarLider(@PathVariable Long id,
                                                      @RequestBody(required = false) UsarLiderRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<PartidaDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.usarLider(actor.getUserId(), id, request))
                .status(HttpStatus.OK.value())
                .build();
    }
}
