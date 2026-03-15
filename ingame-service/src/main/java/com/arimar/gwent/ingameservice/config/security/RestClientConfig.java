package com.arimar.gwent.ingameservice.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
