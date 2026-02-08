package com.arimar.gwent.bff.controller;

import com.arimar.gwent.bff.client.AuthServiceClient;
import com.arimar.gwent.bff.service.AuthService;
import com.arimar.gwent.common.response.GenericResponseDTO;
import com.arimar.gwent.common.utils.exception.BadRequestException;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${gwent.services.bff.v1-version}")
public class AuthControllerImpl implements AuthController {

    private final AuthService auth;
    private final AuthServiceClient authServiceClient;
    @Value("${spring.application.name}")
    private String serviceOrigin;
    public AuthControllerImpl(AuthService auth, AuthServiceClient authServiceClient) {
        this.auth = auth;
        this.authServiceClient = authServiceClient;

    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public GenericResponseDTO<TokenResponse> register(@Valid @RequestBody RegisterRequest req,
                                                      BindingResult bindingResult) throws BadRequestException {

        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult);
        }
        return GenericResponseDTO.<TokenResponse>builder()
                .data(authServiceClient.register(req).getData())
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CREATED.value())
                .build();
    }

    @Override
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenericResponseDTO<TokenResponse> login(LoginRequest req, BindingResult bindingResult) throws BadRequestException {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult);
        }
        return GenericResponseDTO.<TokenResponse>builder()
                .data(authServiceClient.login(req).getData())
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CREATED.value())
                .build();
    }
}

    /*
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Override
    public GenericResponseDTO<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                                   BindingResult bindingResult) throws BadRequestException {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult);
        }
        return GenericResponseDTO.<TokenResponse>builder()
                .data(auth.login(req))
                .serviceOrigin(serviceOrigin)
                .status(HttpStatus.CREATED.value())
                .build();
    }

      */

