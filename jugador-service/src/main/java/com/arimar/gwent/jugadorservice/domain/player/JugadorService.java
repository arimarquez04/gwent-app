package com.arimar.gwent.jugadorservice.domain.player;

import com.arimar.gwent.jugadorservice.dto.PlayerProfileDTO;
import com.arimar.gwent.jugadorservice.dto.UpdatePlayerRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class JugadorService {

    private final JugadorRepository jugadorRepository;

    public JugadorService(JugadorRepository jugadorRepository) {
        this.jugadorRepository = jugadorRepository;
    }

    public PlayerProfileDTO createProfile(UUID userId, String apodo) {
        JugadorEntity entity = new JugadorEntity();
        entity.setUserId(userId);
        entity.setApodo(apodo);
        JugadorEntity saved = jugadorRepository.save(entity);
        return toDTO(saved);
    }

    public PlayerProfileDTO getProfile(UUID userId) {
        JugadorEntity entity = jugadorRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player profile not found"));
        return toDTO(entity);
    }

    public PlayerProfileDTO updateProfile(UUID userId, UpdatePlayerRequest req) {
        JugadorEntity entity = jugadorRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player profile not found"));
        if (req.getApodo() != null) {
            entity.setApodo(req.getApodo());
        }
        if (req.getAvatarUrl() != null) {
            entity.setAvatarUrl(req.getAvatarUrl());
        }
        try {
            return toDTO(jugadorRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apodo already taken");
        }
    }

    private PlayerProfileDTO toDTO(JugadorEntity e) {
        return PlayerProfileDTO.builder()
                .userId(e.getUserId())
                .apodo(e.getApodo())
                .avatarUrl(e.getAvatarUrl())
                .nivel(e.getNivel())
                .victorias(e.getVictorias())
                .derrotas(e.getDerrotas())
                .empates(e.getEmpates())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
