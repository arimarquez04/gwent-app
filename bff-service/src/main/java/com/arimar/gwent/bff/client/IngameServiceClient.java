package com.arimar.gwent.bff.client;

import com.arimar.gwent.bff.client.config.IngameServiceConfig;
import com.arimar.gwent.bff.dto.ingame.*;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.communication.error.RemoteServiceErrorMapper;
import com.arimar.gwent.communication.invoker.GenericRestInvoker;
import com.arimar.gwent.communication.invoker.ServiceCallResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IngameServiceClient {

    private final IngameServiceConfig cfg;
    private final GenericRestInvoker invoker;
    private final RemoteServiceErrorMapper errorMapper;

    public IngameServiceClient(IngameServiceConfig cfg, GenericRestInvoker invoker, RemoteServiceErrorMapper errorMapper) {
        this.cfg = cfg;
        this.invoker = invoker;
        this.errorMapper = errorMapper;
    }

    public GenericResponseDTO<List<CartaCatalogoDTO>> getCards(
            String faccion, String fila, String tipo, Boolean esHeroe, String habilidad, Boolean esEspecial) {
        StringBuilder url = new StringBuilder(cfg.getBaseUrl() + cfg.getCardsUrl());
        addParam(url, "faccion", faccion);
        addParam(url, "fila", fila);
        addParam(url, "tipo", tipo);
        if (esHeroe != null) {
            addParam(url, "esHeroe", esHeroe.toString());
        }
        addParam(url, "habilidad", habilidad);
        if (esEspecial != null){
            addParam(url, "esEspecial", esEspecial.toString());
        }
        String finalUrl = url.toString();
        ServiceCallResponse<List<CartaCatalogoDTO>> response =
                invoker.exchange(
                HttpMethod.GET,
                        finalUrl,
                        null,
                        Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), finalUrl, response.httpStatus(), response.error());
    }

    private static void addParam(StringBuilder url, String name, String value) {
        if (value != null && !value.isBlank()) {
            url.append(url.indexOf("?") >= 0 ? "&" : "?").append(name).append("=").append(value);
        }
    }

    public GenericResponseDTO<List<CartaJugadorDTO>> unlockCards(UnlockCardsRequest request) {
        String url = cfg.getBaseUrl() + cfg.getPlayerCardsUrl();
        ServiceCallResponse<List<CartaJugadorDTO>> response = invoker.exchange(
                HttpMethod.POST, url, request, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<List<CartaJugadorDTO>> getMyCards() {
        String url = cfg.getBaseUrl() + cfg.getPlayerCardsUrl();
        ServiceCallResponse<List<CartaJugadorDTO>> response = invoker.exchange(
                HttpMethod.GET, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<CartaCatalogoDTO> getCardById(Long id) {
        String url = cfg.getBaseUrl() + cfg.getCardsUrl() + "/" + id;
        ServiceCallResponse<CartaCatalogoDTO> response = invoker.exchange(
                HttpMethod.GET, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<MazoDTO> createMazo(CreateMazoRequest request) {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl();
        ServiceCallResponse<MazoDTO> response = invoker.exchange(
                HttpMethod.POST, url, request, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<List<MazoDTO>> getMazos() {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl();
        ServiceCallResponse<List<MazoDTO>> response = invoker.exchange(
                HttpMethod.GET, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<MazoDTO> getMazoById(Long id) {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl() + "/" + id;
        ServiceCallResponse<MazoDTO> response = invoker.exchange(
                HttpMethod.GET, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<MazoDTO> updateMazo(Long id, UpdateMazoRequest request) {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl() + "/" + id;
        ServiceCallResponse<MazoDTO> response = invoker.exchange(
                HttpMethod.PUT, url, request, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public GenericResponseDTO<MazoDTO> setActiveMazo(Long id) {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl() + "/" + id + "/activate";
        ServiceCallResponse<MazoDTO> response = invoker.exchange(
                HttpMethod.PATCH, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (response.isOk()) return response.ok();
        throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
    }

    public void deleteMazo(Long id) {
        String url = cfg.getBaseUrl() + cfg.getMazosUrl() + "/" + id;
        ServiceCallResponse<Void> response = invoker.exchange(
                HttpMethod.DELETE, url, null, Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (!response.isOk()) {
            throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
        }
    }
}
