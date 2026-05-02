package com.arimar.gwent.ingameservice.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("gwent.services.jugador")
public class JugadorServiceConfig {
    private String name;
    private String baseUrl;
    private String matchResultUrl;
}
