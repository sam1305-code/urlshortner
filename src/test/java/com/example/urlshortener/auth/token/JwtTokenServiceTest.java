package com.example.urlshortener.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.config.JwtProperties;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long";

    @Test
    void createAccessTokenSignsExpectedClaims() {
        JwtProperties properties = new JwtProperties(
                "test-issuer",
                SECRET,
                Duration.ofMinutes(15));
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneOffset.UTC);
        JwtTokenService jwtTokenService = new JwtTokenService(
                properties,
                new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secretKey())),
                fixedClock);
        UserAccount userAccount = new UserAccount(
                UUID.fromString("d4a7bc5d-45cb-4f53-bcc0-e9833e7cd45f"),
                "Samhita",
                "samhita@example.com",
                "$2a$12$passwordHash",
                Instant.parse("2026-07-08T10:00:00Z"));

        AuthToken authToken = jwtTokenService.createAccessToken(userAccount);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey())
                .build();
        jwtDecoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());

        org.springframework.security.oauth2.jwt.Jwt decodedJwt = jwtDecoder.decode(authToken.accessToken());

        assertThat(authToken.tokenType()).isEqualTo("Bearer");
        assertThat(authToken.expiresInSeconds()).isEqualTo(900);
        assertThat(decodedJwt.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(decodedJwt.getSubject()).isEqualTo(userAccount.id().toString());
        assertThat(decodedJwt.getClaimAsString("email")).isEqualTo("samhita@example.com");
        assertThat(decodedJwt.getClaimAsString("name")).isEqualTo("Samhita");
        assertThat(decodedJwt.getIssuedAt()).isEqualTo(Instant.parse("2026-07-08T10:15:30Z"));
        assertThat(decodedJwt.getExpiresAt()).isEqualTo(Instant.parse("2026-07-08T10:30:30Z"));
    }

    private static SecretKey secretKey() {
        return new SecretKeySpec(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }
}
