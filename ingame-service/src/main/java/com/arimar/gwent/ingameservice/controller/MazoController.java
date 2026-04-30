package com.arimar.gwent.ingameservice.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.ingameservice.config.security.ActorResolver;
import com.arimar.gwent.ingameservice.dto.*;
import com.arimar.gwent.ingameservice.service.MazoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingame/v1/players/me/mazos")
@RequiredArgsConstructor
public class MazoController {

    private final MazoService service;
    private final ActorResolver actorResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<MazoDTO> createMazo(@Valid @RequestBody CreateMazoRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<MazoDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.createMazo(actor.getUserId(), request))
                .status(HttpStatus.CREATED.value())
                .build();
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<MazoDTO>> getMazos() {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<List<MazoDTO>>builder()
                .serviceOrigin(serviceName)
                .data(service.getMazos(actor.getUserId()))
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<MazoDTO> getMazoById(@PathVariable Long id) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<MazoDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.getMazoById(actor.getUserId(), id))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<MazoDTO> updateMazo(@PathVariable Long id,
                                                   @RequestBody UpdateMazoRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<MazoDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.updateMazo(actor.getUserId(), id, request))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<MazoDTO> setActiveMazo(@PathVariable Long id) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<MazoDTO>builder()
                .serviceOrigin(serviceName)
                .data(service.setActiveMazo(actor.getUserId(), id))
                .status(HttpStatus.OK.value())
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMazo(@PathVariable Long id) {
        var actor = actorResolver.currentActor();
        service.deleteMazo(actor.getUserId(), id);
    }
}
