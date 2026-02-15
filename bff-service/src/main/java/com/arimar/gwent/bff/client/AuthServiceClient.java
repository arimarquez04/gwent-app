package com.arimar.gwent.bff.client;

import com.arimar.gwent.bff.client.config.AuthServiceConfig;

import com.arimar.gwent.communication.error.RemoteServiceErrorMapper;
import com.arimar.gwent.communication.error.RemoteServiceErrorMapperImpl;
import com.arimar.gwent.communication.invoker.GenericRestInvoker;
import com.arimar.gwent.communication.invoker.GenericRestInvokerImpl;
import com.arimar.gwent.communication.invoker.ServiceCallResponse;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


import  com.arimar.gwent.common.response.GenericResponseDTO;

import java.util.Map;


@Component
@Slf4j
public class AuthServiceClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuthServiceConfig cfg;
    private final GenericRestInvoker invoker;
    private final RemoteServiceErrorMapper errorMapper;

    public AuthServiceClient(RestClient.Builder builder, ObjectMapper objectMapper, AuthServiceConfig authServiceConfig, GenericRestInvoker invoker, RemoteServiceErrorMapper errorMapper) {
        this.objectMapper = objectMapper;
        this.cfg = authServiceConfig;
        this.restClient = builder
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.invoker = invoker;
        this.errorMapper = errorMapper;
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

    public GenericResponseDTO<TokenResponse> registerWithInvoker(RegisterRequest request) {
        String url = cfg.getBaseUrl() + cfg.getV1Version() + cfg.getRegister();
        ParameterizedTypeReference<GenericResponseDTO<TokenResponse>> okType =
                new ParameterizedTypeReference<>() {};
        ServiceCallResponse<TokenResponse> response =
                invoker.exchange(
                        HttpMethod.POST,
                        url,
                        request,
                        Map.of(),
                        okType
                );
        if (response.isOk()) {
            return response.ok();
        }
       throw  errorMapper.toException(
               cfg.getName(),
               url,
               response.httpStatus(),
               response.error());
    }

    public GenericResponseDTO<TokenResponse> loginWithInvoker(LoginRequest request) {
        String url = cfg.getBaseUrl() + cfg.getV1Version() + cfg.getLogin();
        ParameterizedTypeReference<GenericResponseDTO<TokenResponse>> okType =
                new ParameterizedTypeReference<>() {};
        ServiceCallResponse<TokenResponse> response =
                invoker.exchange(
                        HttpMethod.POST,
                        url,
                        request,
                        Map.of(),
                        okType
                );
        if (response.isOk()) {
            return response.ok();
        }
        throw  errorMapper.toException(
                cfg.getName(),
                url,
                response.httpStatus(),
                response.error());
    }

}