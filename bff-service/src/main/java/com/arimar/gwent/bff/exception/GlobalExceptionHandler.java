package com.arimar.gwent.bff.exception;

import com.arimar.gwent.common.exception.ErrorDTO;
import com.arimar.gwent.common.exception.InternalService4xxErrorException;
import com.arimar.gwent.common.exception.InternalService5xxErrorException;
import com.arimar.gwent.common.utils.exception.BadRequestException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String serviceOrigin;

    /**
     * Errores 4xx provenientes de servicios internos (jugador-service, auth-service).
     * Propaga el ErrorDTO original con el status real del servicio upstream.
     */
    @ExceptionHandler(InternalService4xxErrorException.class)
    public ErrorDTO internalService4xx(InternalService4xxErrorException ex, HttpServletResponse response) {
        HttpStatusCode status = ex.getHttpStatus();
        response.setStatus(status.value());
        ErrorDTO upstream = ex.getError();
        if (upstream != null) return upstream;
        return ErrorDTO.builder()
                .serviceOrigin(ex.getServiceErrorOrigin())
                .status(status)
                .message(ex.getMessage())
                .build();
    }

    /**
     * Errores 5xx provenientes de servicios internos.
     */
    @ExceptionHandler(InternalService5xxErrorException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorDTO internalService5xx(InternalService5xxErrorException ex) {
        log.error("5xx from internal service={} status={}", ex.getServiceErrorOrigin(), ex.getHttpStatus());
        ErrorDTO upstream = ex.getError();
        if (upstream != null) return upstream;
        return ErrorDTO.builder()
                .serviceOrigin(ex.getServiceErrorOrigin())
                .status(HttpStatus.BAD_GATEWAY)
                .message("Error en servicio interno: " + ex.getServiceErrorOrigin())
                .build();
    }

    /**
     * Errores de validacion de request (BindingResult).
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO badRequest(BadRequestException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.BAD_REQUEST)
                .message(message)
                .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO typeMismatch(MethodArgumentTypeMismatchException ex) {
        String allowed = ex.getRequiredType() != null && ex.getRequiredType().isEnum()
                ? " Valores permitidos: " + java.util.Arrays.toString(ex.getRequiredType().getEnumConstants())
                : "";
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.BAD_REQUEST)
                .message("Valor inválido '" + ex.getValue() + "' para el parámetro '" + ex.getName() + "'." + allowed)
                .build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ErrorDTO responseStatus(ResponseStatusException ex, HttpServletResponse response) {
        HttpStatusCode status = ex.getStatusCode();
        response.setStatus(status.value());
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(status)
                .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO generic(Exception ex) {
        log.error("Unhandled exception", ex);
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Internal server error")
                .build();
    }
}
