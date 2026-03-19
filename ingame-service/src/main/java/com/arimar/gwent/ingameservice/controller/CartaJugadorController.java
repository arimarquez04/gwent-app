package com.arimar.gwent.ingameservice.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.ingameservice.config.security.ActorResolver;
import com.arimar.gwent.ingameservice.dto.CartaJugadorDTO;
import com.arimar.gwent.ingameservice.dto.UnlockCardsRequest;
import com.arimar.gwent.ingameservice.service.CartaJugadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingame/v1/players/me/cards")
@RequiredArgsConstructor
public class CartaJugadorController {

    private final CartaJugadorService service;
    private final ActorResolver actorResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaJugadorDTO>> unlockCards(@RequestBody UnlockCardsRequest request) {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<List<CartaJugadorDTO>>builder()
                .serviceOrigin(serviceName)
                .data(service.unlockCards(actor.getUserId(), request.getCardIds()))
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaJugadorDTO>> getMyCards() {
        var actor = actorResolver.currentActor();
        return GenericResponseDTO.<List<CartaJugadorDTO>>builder()
                .serviceOrigin(serviceName)
                .data(service.getMyCards(actor.getUserId()))
                .status(HttpStatus.OK.value())
                .build();
    }
}
