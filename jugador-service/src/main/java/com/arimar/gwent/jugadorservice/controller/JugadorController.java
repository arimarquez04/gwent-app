package com.arimar.gwent.jugadorservice.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.jugadorservice.config.security.ActorResolver;
import com.arimar.gwent.jugadorservice.domain.player.JugadorService;
import com.arimar.gwent.jugadorservice.dto.CreatePlayerRequest;
import com.arimar.gwent.jugadorservice.dto.PlayerProfileDTO;
import com.arimar.gwent.security.actor.Actor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class JugadorController {

    private final JugadorService jugadorService;
    private final ActorResolver actorResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    public JugadorController(JugadorService jugadorService, ActorResolver actorResolver) {
        this.jugadorService = jugadorService;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<PlayerProfileDTO> createProfile(@Valid @RequestBody CreatePlayerRequest req,
                                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        Actor actor = actorResolver.currentActor();
        PlayerProfileDTO profile = jugadorService.createProfile(actor.getUserId(), req.getApodo());
        return GenericResponseDTO.<PlayerProfileDTO>builder()
                .serviceOrigin(serviceName)
                .status(HttpStatus.CREATED.value())
                .data(profile)
                .build();
    }
}
