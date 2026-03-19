package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.List;

@Data
public class UnlockCardsRequest {
    private List<Long> cardIds;
}
