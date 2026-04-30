package com.arimar.gwent.ingameservice.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MulliganRequest {

    private List<Long> cartaPartidaIds = new ArrayList<>();
}
