package com.arimar.gwent.ingame_service.controller;


import com.arimar.gwent.ingame_service.dto.requets.UserRequestDTO;
import com.arimar.gwent.ingame_service.dto.response.GenericResponseDTO;
import com.arimar.gwent.ingame_service.dto.response.UserResponseDTO;
import com.arimar.gwent.ingame_service.exception.BadRequestException;
import com.arimar.gwent.ingame_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("api/")
public class UserController {

    private UserService userService;
    @Value("${spring.application.name}")
    private String SERVICE_NAME;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("v1/user")
    @Deprecated(since = "13/03/2025", forRemoval = true)
    public ResponseEntity<GenericResponseDTO<UserResponseDTO>> signUp(@Valid @RequestBody UserRequestDTO requestDTO, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", "));

            GenericResponseDTO<String> response = new GenericResponseDTO<>(SERVICE_NAME, HttpStatus.BAD_REQUEST.value(), errorMessage);
            return ResponseEntity.badRequest().build();
        }

        UserResponseDTO responseDTO = userService.save(requestDTO);
        GenericResponseDTO<UserResponseDTO> response = new GenericResponseDTO<>(
                SERVICE_NAME,
                HttpStatus.OK.value(),
                responseDTO
        );
        return ResponseEntity.ok(response);
    }
    @PostMapping("v2/user")
    public ResponseEntity<GenericResponseDTO<UserResponseDTO>> signUpV2(@Valid @RequestBody UserRequestDTO requestDTO, BindingResult bindingResult) throws BadRequestException {

        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult);
        }

        UserResponseDTO responseDTO = userService.save(requestDTO);
        GenericResponseDTO<UserResponseDTO> response = new GenericResponseDTO<>(
                SERVICE_NAME,
                HttpStatus.OK.value(),
                responseDTO
        );
        return ResponseEntity.ok(response);
    }
}
