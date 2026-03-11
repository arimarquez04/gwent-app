package com.arimar.gwent.bff.client;

import com.arimar.gwent.bff.client.config.JugadorServiceConfig;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.communication.error.RemoteServiceErrorMapper;
import com.arimar.gwent.communication.invoker.GenericRestInvoker;
import com.arimar.gwent.communication.invoker.ServiceCallResponse;
import com.arimar.gwent.security.actor.Actor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
public class JugadorServiceClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final JugadorServiceConfig cfg;
    private final GenericRestInvoker invoker;
    private final RemoteServiceErrorMapper errorMapper;

    public JugadorServiceClient(RestClient.Builder builder, ObjectMapper objectMapper, JugadorServiceConfig jugadorServiceConfig, GenericRestInvoker invoker, RemoteServiceErrorMapper errorMapper) {
        this.objectMapper = objectMapper;
        this.cfg = jugadorServiceConfig;
        this.restClient = builder
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.invoker = invoker;
        this.errorMapper = errorMapper;
    }




    public void createProfile(String apodo, String bearerToken) {
        String url = cfg.getBaseUrl() + cfg.getCreatePlayerUrl();
        ParameterizedTypeReference<GenericResponseDTO<Object>> okType =
                new ParameterizedTypeReference<>() {};
        ServiceCallResponse<Object> response =
                invoker.exchange(
                        HttpMethod.POST,
                        url,
                        Map.of("apodo", apodo),
                        Map.of("Authorization", "Bearer " + bearerToken),
                        okType
                );
        if (!response.isOk()) {
            log.error("Failed to create jugador profile for apodo={} status={} error={}",
                    apodo, response.httpStatus(), response.error());
            throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
        }
    }

    public GenericResponseDTO<Actor> me() {
        String url = cfg.getBaseUrl() + cfg.getMeUrl();
        ParameterizedTypeReference<GenericResponseDTO<Actor>> okType =
                new ParameterizedTypeReference<>() {};
        ServiceCallResponse<Actor> response =
                invoker.exchange(
                        HttpMethod.GET,
                        url,
                        null,
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