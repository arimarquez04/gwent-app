package com.arimar.gwent.bff.security;

import com.arimar.gwent.security.actor.Actor;
import com.arimar.gwent.contracts.auth.claims.JwtClaimNames;
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

        UUID userId = UUID.fromString(jwt.getClaimAsString(JwtClaimNames.USER_ID));
        String gameId = jwt.getClaimAsString(JwtClaimNames.GAME_ID);
        String username = jwt.getClaimAsString(JwtClaimNames.USERNAME);
        String tag = jwt.getClaimAsString(JwtClaimNames.TAG);

        return Actor.builder()
                .userId(userId)
                .gameId(gameId)
                .username(username)
                .tag(tag)
                .build();
    }

    public String bearerTokenValue() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) return jwt.getTokenValue();
        throw new IllegalStateException("No Jwt principal in SecurityContext");
    }
}
