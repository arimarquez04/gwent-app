package com.arimar.gwent.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> unauthorized(UnauthorizedException ex) {
        return Map.of("error", "unauthorized", "message", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> conflict(ConflictException ex) {
        return Map.of("error", "conflict", "message", ex.getMessage());
    }
}
