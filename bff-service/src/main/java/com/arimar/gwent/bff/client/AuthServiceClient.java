package com.arimar.gwent.bff.client;

import com.arimar.gwent.bff.client.config.AuthServiceConfig;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


import  com.arimar.gwent.common.response.GenericResponseDTO;


@Component
@Slf4j
public class AuthServiceClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuthServiceConfig cfg;

    public AuthServiceClient(RestClient.Builder builder, ObjectMapper objectMapper, AuthServiceConfig authServiceConfig) {
        this.objectMapper = objectMapper;
        this.cfg = authServiceConfig;
        this.restClient = builder
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


    public GenericResponseDTO<TokenResponse> register(RegisterRequest request) {
        String url = cfg.getBaseUrl() + cfg.getV1Version() + cfg.getRegister();
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<GenericResponseDTO<TokenResponse>>() {});
    }
    public GenericResponseDTO<TokenResponse> login(LoginRequest request) {
        String url = cfg.getBaseUrl() + cfg.getV1Version() + cfg.getLogin();
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<GenericResponseDTO<TokenResponse>>() {});
    }

}