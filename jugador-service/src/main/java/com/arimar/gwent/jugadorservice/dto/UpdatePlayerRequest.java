package com.arimar.gwent.jugadorservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePlayerRequest {

    @Size(min = 3, max = 30, message = "Apodo must be between 3 and 30 characters")
    private String apodo;

    private String avatarUrl;
}
