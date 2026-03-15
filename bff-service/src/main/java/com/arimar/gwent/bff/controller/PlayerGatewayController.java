package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.JugadorServiceClient;
import com.arimar.gwent.bff.dto.jugador.PlayerProfileDTO;
import com.arimar.gwent.bff.dto.jugador.UpdateProfileRequest;
import com.arimar.gwent.bff.security.ActorResolver;
import com.arimar.gwent.common.exception.InternalService4xxErrorException;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.security.actor.Actor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
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

    @GetMapping("/players/me")
    public GenericResponseDTO<PlayerProfileDTO> getPlayerProfile() {
        try {
            return jugadorServiceClient.getProfile();
        } catch (InternalService4xxErrorException e) {
            if (e.getHttpStatus().value() != 404) throw e;
            log.info("Player profile not found, creating lazily for userId={}",
                    actorResolver.currentActor().getUserId());
            jugadorServiceClient.createProfileLazy(actorResolver.currentActor().getUsername());
            return jugadorServiceClient.getProfile();
        }
    }

    @PatchMapping("/players/me")
    public GenericResponseDTO<PlayerProfileDTO> updatePlayerProfile(@RequestBody UpdateProfileRequest req) {
        return jugadorServiceClient.updateProfile(req);
    }
}
