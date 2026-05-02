package com.arimar.gwent.bff.dto.ingame;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MulliganRequest {

    private List<Long> cartaPartidaIds = new ArrayList<>();
}
