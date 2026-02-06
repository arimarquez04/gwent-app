package com.arimar.gwent.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gwent.jwt")
public record JWTConfigurationProperties(
        String issuer,
        String audience,
        long ttlSeconds,
        String privateKeyPem,
        String publicKeyPem
) {}
