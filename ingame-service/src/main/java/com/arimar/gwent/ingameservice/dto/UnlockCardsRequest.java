package com.arimar.gwent.ingameservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnlockCardsRequest {
    private List<Long> cardIds;
}
