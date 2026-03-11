package com.arimar.gwent.jugadorservice.controller;


import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.jugadorservice.config.security.ActorResolver;
import com.arimar.gwent.security.actor.Actor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlayerGatewayController {

    private final ActorResolver actorResolver;
    @Value("${spring.application.name}")
    private String serviceName;
    public PlayerGatewayController(ActorResolver actorResolver) {
        this.actorResolver = actorResolver;
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<Actor> me() {
        // útil para testear extracción de claims
        return new GenericResponseDTO<Actor>(
                serviceName,
                HttpStatus.OK.value(),
                actorResolver.currentActor()
        );
    }
}
