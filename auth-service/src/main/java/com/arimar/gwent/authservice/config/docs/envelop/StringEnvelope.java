package com.arimar.gwent.authservice.config.docs.envelop;

import com.arimar.gwent.common.response.GenericResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StringEnvelope")
public class StringEnvelope extends GenericResponseDTO<String> {}
