package com.arimar.gwent.authservice.exception;

import com.arimar.gwent.common.response.GenericResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @Value("${spring.application.name}")
    private String serviceOrigin;

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public GenericResponseDTO<String> unauthorized(UnauthorizedException ex) {
        return GenericResponseDTO.<String>builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CONFLICT.value())
                .data("Credenciales invalidas")
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public GenericResponseDTO<String> unauthorized(ConflictException ex) {
        return GenericResponseDTO.<String>builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.UNAUTHORIZED.value())
                .data(ex.getMessage())
                .build();
    }
}
