package com.arimar.gwent.ingameservice.config.security;

import com.arimar.gwent.contracts.auth.claims.JwtClaimNames;
import com.arimar.gwent.security.actor.Actor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActorResolver {

    public Actor currentActor() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException("No Jwt principal in SecurityContext");
        }
        return Actor.builder()
                .userId(UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID)))
                .gameId(jwt.getClaimAsString(JwtClaimNames.GAME_ID))
                .username(jwt.getClaimAsString(JwtClaimNames.USERNAME))
                .tag(jwt.getClaimAsString(JwtClaimNames.TAG))
                .build();
    }
}
