package com.arimar.gwent.bff.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "gwent.services.ingame")
@Configuration
public class IngameServiceConfig {
    private String name;
    private String baseUrl;
    private String cardsUrl;
    private String playerCardsUrl;
}
