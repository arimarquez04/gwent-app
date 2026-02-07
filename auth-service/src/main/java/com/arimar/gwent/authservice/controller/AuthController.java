package com.arimar.gwent.authservice.controller;

import com.arimar.gwent.authservice.config.docs.envelop.StringEnvelope;
import com.arimar.gwent.authservice.config.docs.envelop.TokenResponseEnvelope;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.common.utils.exception.BadRequestException;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Tag(name = "Auth", description = "Operaciones de registro e inicio de sesion de usuario")
public interface AuthController {

    @Operation(summary = "Registro de usuario", description = "Crea un nuevo usuario. Valida el request y devuelve el token."
    )
    @ApiResponse(responseCode = "201", description = "Usuario creado",
            content = @Content(schema = @Schema(implementation = TokenResponseEnvelope.class))
    )
    @ApiResponse(responseCode = "400", description = "Request inválido",
            content = @Content(schema = @Schema(implementation = StringEnvelope.class))
    )
    @ApiResponse(responseCode = "500", description = "Error Interno",
            content = @Content(schema = @Schema(implementation = StringEnvelope.class))
    )
    @PostMapping("/register")
    GenericResponseDTO<TokenResponse> register(
            @Valid @RequestBody RegisterRequest req,
            BindingResult bindingResult
    ) throws BadRequestException;

    @Operation(summary = "Login de usuario", description = "Valida el request y devuelve el token.")
    @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = TokenResponseEnvelope.class))
    )
    @ApiResponse(responseCode = "400", description = "Request inválido",
            content = @Content(schema = @Schema(implementation = StringEnvelope.class))
    )
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = StringEnvelope.class))
    )
    @ApiResponse(responseCode = "500", description = "Error Interno",
            content = @Content(schema = @Schema(implementation = StringEnvelope.class))
    )
    @PostMapping("/login")
    GenericResponseDTO<TokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            BindingResult bindingResult
    ) throws BadRequestException;

}