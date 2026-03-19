package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.IngameServiceClient;
import com.arimar.gwent.bff.dto.ingame.CartaJugadorDTO;
import com.arimar.gwent.bff.dto.ingame.UnlockCardsRequest;
import com.arimar.gwent.common.response.GenericResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players/me/cards")
@RequiredArgsConstructor
public class PlayerCardGatewayController {

    private final IngameServiceClient ingameClient;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaJugadorDTO>> unlockCards(@RequestBody UnlockCardsRequest request) {
        return ingameClient.unlockCards(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GenericResponseDTO<List<CartaJugadorDTO>> getMyCards() {
        return ingameClient.getMyCards();
    }
}
