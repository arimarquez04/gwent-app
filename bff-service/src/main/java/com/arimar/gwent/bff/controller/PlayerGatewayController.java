package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.JugadorServiceClient;
import com.arimar.gwent.bff.security.ActorResolver;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.security.actor.Actor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PlayerGatewayController {

    private final ActorResolver actorResolver;
    private final JugadorServiceClient jugadorServiceClient;
    public PlayerGatewayController(ActorResolver actorResolver, JugadorServiceClient jugadorServiceClient) {
        this.actorResolver = actorResolver;
        this.jugadorServiceClient = jugadorServiceClient;
    }

    @GetMapping("/me")
    public Actor me() {
        // útil para testear extracción de claims
        return actorResolver.currentActor();
    }

    @GetMapping("/jugador-service/me")
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<Actor> meFromJugadorService() {
        return new GenericResponseDTO<>(
                "jugador-service",
                HttpStatus.OK.value(),
                jugadorServiceClient.me().getData()
        );
    }
}
