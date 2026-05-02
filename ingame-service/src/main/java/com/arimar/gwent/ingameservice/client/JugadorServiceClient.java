package com.arimar.gwent.ingameservice.client;

import com.arimar.gwent.communication.error.RemoteServiceErrorMapper;
import com.arimar.gwent.communication.invoker.GenericRestInvoker;
import com.arimar.gwent.communication.invoker.ServiceCallResponse;
import com.arimar.gwent.ingameservice.client.config.JugadorServiceConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JugadorServiceClient {

    private final JugadorServiceConfig cfg;
    private final GenericRestInvoker invoker;
    private final RemoteServiceErrorMapper errorMapper;

    public void reportMatchResult(UUID jugadorUnoId, UUID jugadorDosId, UUID ganadorId, boolean empate) {
        String url = cfg.getBaseUrl() + cfg.getMatchResultUrl();
        MatchResultRequest body = new MatchResultRequest(jugadorUnoId, jugadorDosId, ganadorId, empate);
        ServiceCallResponse<Void> response = invoker.exchange(
                HttpMethod.POST, url, body, java.util.Map.of(),
                new ParameterizedTypeReference<>() {}
        );
        if (!response.isOk()) {
            throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
        }
    }

    @Data
    private static class MatchResultRequest {
        private final UUID jugadorUnoId;
        private final UUID jugadorDosId;
        private final UUID ganadorId;
        private final boolean empate;
    }
}
