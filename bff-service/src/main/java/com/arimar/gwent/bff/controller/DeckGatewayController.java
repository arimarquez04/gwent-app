package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.IngameServiceClient;
import com.arimar.gwent.bff.dto.ingame.CreateMazoRequest;
import com.arimar.gwent.bff.dto.ingame.MazoDTO;
import com.arimar.gwent.bff.dto.ingame.UpdateMazoRequest;
import com.arimar.gwent.common.response.GenericResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players/me/mazos")
@RequiredArgsConstructor
public class DeckGatewayController {

    private final IngameServiceClient ingameClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<MazoDTO> createMazo(@Valid @RequestBody CreateMazoRequest request) {
        return ingameClient.createMazo(request);
    }

    @GetMapping
    public GenericResponseDTO<List<MazoDTO>> getMazos() {
        return ingameClient.getMazos();
    }

    @GetMapping("/{id}")
    public GenericResponseDTO<MazoDTO> getMazoById(@PathVariable Long id) {
        return ingameClient.getMazoById(id);
    }

    @PutMapping("/{id}")
    public GenericResponseDTO<MazoDTO> updateMazo(@PathVariable Long id,
                                                   @RequestBody UpdateMazoRequest request) {
        return ingameClient.updateMazo(id, request);
    }

    @PatchMapping("/{id}/activate")
    public GenericResponseDTO<MazoDTO> setActiveMazo(@PathVariable Long id) {
        return ingameClient.setActiveMazo(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMazo(@PathVariable Long id) {
        ingameClient.deleteMazo(id);
    }
}
