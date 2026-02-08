package com.arimar.gwent.bff.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "gwent.services.auth")
@Configuration
public class AuthServiceConfig {
    private String name;
    private String baseUrl;
    private String v1Version;
    private String register;
    private String login;
}

