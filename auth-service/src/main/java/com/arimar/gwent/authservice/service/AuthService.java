package com.arimar.gwent.authservice.service;

import com.arimar.gwent.authservice.exception.ConflictException;
import com.arimar.gwent.authservice.exception.UnauthorizedException;
import com.arimar.gwent.authservice.repository.UserEntityRepository;
import com.arimar.gwent.authservice.security.JwtService;
import com.arimar.gwent.contracts.auth.request.LoginRequest;
import com.arimar.gwent.contracts.auth.request.RegisterRequest;
import com.arimar.gwent.contracts.auth.response.TokenResponse;
import com.arimar.gwent.domain.user.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserEntityRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserEntityRepository repo, PasswordEncoder encoder, JwtService jwtService) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public TokenResponse register(RegisterRequest req) {
        if (repo.existsByUsername(req.getUsername())){
            throw new ConflictException("username already exists");
        }
        if (req.getEmail() != null && repo.existsByEmail(req.getEmail())){
            throw new ConflictException("email already exists");
        }
        if (repo.existsByGameIdAndTag(req.getGameId(), req.getTag())) {
            throw new ConflictException("gameId#tag already exists");
        }

        UserEntity newUser = UserEntity.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .gameId(req.getGameId())
                .tag(req.getTag())
                .password(encoder.encode(req.getPassword()))
                .build();

        UserEntity saved = repo.save(newUser);

        JwtService.TokenIssued issued = jwtService.issueToken(saved);
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(issued.token());
        //tokenResponse.setTokenType("Bearer")
        tokenResponse.setExpiresIn(issued.expiresInSeconds());
        return tokenResponse;    }

    public TokenResponse login(LoginRequest req) {
        UserEntity acc = findByIdentifier(req.getIdentifier())
            .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!encoder.matches(req.getPassword(), acc.getPassword())) {
            throw new UnauthorizedException("invalid credentials");
        }

        JwtService.TokenIssued issued = jwtService.issueToken(acc);
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(issued.token());
        //tokenResponse.setTokenType("Bearer")
        tokenResponse.setExpiresIn(issued.expiresInSeconds());
        return tokenResponse;
    }

    private java.util.Optional<UserEntity> findByIdentifier(String identifier) {
        if (identifier == null) return java.util.Optional.empty();

        if (identifier.contains("@")) {
            return repo.findByEmail(identifier);
        }
        if (identifier.contains("#")) {
            String[] parts = identifier.split("#", 2);
            if (parts.length != 2) return java.util.Optional.empty();
            return repo.findByGameIdAndTag(parts[0], parts[1]);
        }
        return repo.findByUsername(identifier);
    }

}
