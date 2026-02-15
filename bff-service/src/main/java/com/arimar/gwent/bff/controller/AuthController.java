package com.arimar.gwent.bff.controller;

import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.common.utils.exception.BadRequestException;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @ApiResponse(responseCode = "201", description = "Usuario creado"
    )
    @ApiResponse(responseCode = "400", description = "Request inválido"
    )
    @ApiResponse(responseCode = "500", description = "Error Interno"
    )
    @PostMapping("/register")
    GenericResponseDTO<TokenResponse> register(
            @Valid @RequestBody RegisterRequest req,
            BindingResult bindingResult
    ) throws BadRequestException;

    @Operation(summary = "Login de usuario", description = "Valida el request y devuelve el token.")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @ApiResponse(responseCode = "500", description = "Error Interno")
    @PostMapping("/login")
    GenericResponseDTO<TokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            BindingResult bindingResult
    ) throws BadRequestException;

}