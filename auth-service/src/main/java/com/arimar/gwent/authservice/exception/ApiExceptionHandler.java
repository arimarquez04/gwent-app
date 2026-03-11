package com.arimar.gwent.authservice.exception;

import com.arimar.gwent.common.exception.ErrorDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestControllerAdvice
public class ApiExceptionHandler {
    @Value("${spring.application.name}")
    private String serviceOrigin;

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDTO conflict(UnauthorizedException ex) {
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CONFLICT)
                .message("Credenciales invalidas")
                .body(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDTO unauthorized(ConflictException ex) {
         return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.UNAUTHORIZED)
                .message("Credenciales invalidas")
                .body(ex.getMessage())
                .build();
    }
}
