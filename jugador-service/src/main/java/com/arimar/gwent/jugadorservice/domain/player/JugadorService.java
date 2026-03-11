package com.arimar.gwent.jugadorservice.domain.player;

import com.arimar.gwent.jugadorservice.dto.PlayerProfileDTO;
import org.springframework.stereotype.Service;

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
