package com.arimar.gwent.ingameservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UsarLiderRequest {
    private Long targetCartaPartidaId;  // LIDER_STEAL_GRAVEYARD, LIDER_RESTORE_GRAVEYARD
    private List<Long> descartarIds;    // LIDER_DISCARD_DRAW: exactly 2 IDs
}
