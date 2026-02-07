package com.arimar.gwent.authservice.security;

import com.arimar.gwent.authservice.config.security.JWTConfigurationProperties;
import com.arimar.gwent.contracts.auth.claims.JwtClaimNames;
import com.arimar.gwent.domain.user.UserEntity;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JWTConfigurationProperties props;

    public JwtService(ResourceLoader loader, JWTConfigurationProperties props) {
        this.props = props;

        // 1) leer claves PEM
        RSAPrivateKey privateKey = PemKeys.readPrivateKey(loader, props.privateKeyPem());
        RSAPublicKey  publicKey  = PemKeys.readPublicKey(loader, props.publicKeyPem());

        // 2) armar JWK (Nimbus)
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("gwent-auth-key") // id de la key (útil para rotación futura)
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        // 3) encoder RS256
        var jwkSource = new ImmutableJWKSet<SecurityContext>(jwkSet);
        this.encoder = new NimbusJwtEncoder(jwkSource);
    }

    public TokenIssued issueToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.ttlSeconds());

        String userId = user.getUserId().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .issuedAt(now)
                .expiresAt(exp)
                .subject(userId)               // sub = playerId
                .id(UUID.randomUUID().toString())// jti
                // domain claims (los tuyos)
                .claim(JwtClaimNames.USER_ID, userId)
                .claim(JwtClaimNames.USERNAME, user.getUsername())
                .claim(JwtClaimNames.GAME_ID, user.getGameId())
                .claim(JwtClaimNames.TAG, user.getTag())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenIssued(token, props.ttlSeconds());
    }

    public record TokenIssued(String token, long expiresInSeconds) {}
}
