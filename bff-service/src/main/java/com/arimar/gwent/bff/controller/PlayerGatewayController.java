package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.security.ActorResolver;
import com.arimar.gwent.security.actor.Actor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PlayerGatewayController {

    private final ActorResolver actorResolver;

    public PlayerGatewayController(ActorResolver actorResolver) {
        this.actorResolver = actorResolver;
    }

    @GetMapping("/me")
    public Actor me() {
        // útil para testear extracción de claims
        return actorResolver.currentActor();
    }
}
