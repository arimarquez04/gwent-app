package com.arimar.gwent.ingame_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arimar.gwent.ingame_service.dto.response.GenericResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String SERVICE_NAME;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponseDTO<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        GenericResponseDTO<Map<String, String>> response = new GenericResponseDTO<>(
                SERVICE_NAME,
                HttpStatus.BAD_REQUEST.value(),
                errors
        );
        logger.error(ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<GenericResponseDTO<Map<String, String>>> handleBadRequestException(BadRequestException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        GenericResponseDTO<Map<String, String>> response = new GenericResponseDTO<>(
                "SERVICE_NAME",
                HttpStatus.BAD_REQUEST.value(),
                errors
        );
        try {
            String jsonErrors = objectMapper.writeValueAsString(errors);
            logger.info("Validation errors: {}", jsonErrors);
        } catch (JsonProcessingException e) {
            logger.error("Error converting errors map to JSON", e);
        }
        ex.printStackTrace();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponseDTO<String>> handleGenericException(Exception ex) {
        GenericResponseDTO<String> response = new GenericResponseDTO<>(
                SERVICE_NAME,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}