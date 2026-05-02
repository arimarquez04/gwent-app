package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.List;

@Data
public class UsarLiderRequest {
    private Long targetCartaPartidaId;
    private List<Long> descartarIds;
}
