package com.arimar.gwent.authservice.config.docs.envelop;

import io.swagger.v3.oas.annotations.media.Schema;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.contracts.auth.response.TokenResponse;

@Schema(name = "TokenResponseEnvelope")
public class TokenResponseEnvelope extends GenericResponseDTO<TokenResponse> {}

