package com.arimar.gwent.ingame_service.controller;

import com.arimar.gwent.common.user.dto.request.UserRequestDTO;
import com.arimar.gwent.common.user.dto.response.UserResponseDTO;
import com.arimar.gwent.common.utils.exception.BadRequestException;
import com.arimar.gwent.common.utils.response.GenericResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Users", description = "Operaciones de usuarios")
public interface UserControllerApi {

    @Operation(
            summary = "Registro de usuario",
            description = "Crea un nuevo usuario. Valida el request y devuelve el usuario creado."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuario creado",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Request inválido")
    @PostMapping("/v2/signup")
    ResponseEntity<GenericResponseDTO<UserResponseDTO>> signUpV2(
            @Valid @RequestBody UserRequestDTO requestDTO,
            BindingResult bindingResult
    ) throws BadRequestException;
}
