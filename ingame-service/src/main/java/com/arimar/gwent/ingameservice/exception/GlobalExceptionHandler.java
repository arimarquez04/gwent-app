package com.arimar.gwent.ingameservice.exception;

import com.arimar.gwent.common.exception.ErrorDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String serviceOrigin;

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDTO dataIntegrity(DataIntegrityViolationException ex) {
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CONFLICT)
                .message("Data integrity violation")
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO illegalArgument(IllegalArgumentException ex) {
        return ErrorDTO.builder()
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
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
