package com.example.urlshortener.auth.token;

import java.time.Clock;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    @Autowired
    public JwtTokenService(JwtProperties jwtProperties) {
        this(jwtProperties, jwtEncoder(jwtProperties), Clock.systemUTC());
    }

    JwtTokenService(JwtProperties jwtProperties, JwtEncoder jwtEncoder, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public AuthToken createAccessToken(UserAccount userAccount) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(userAccount.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", userAccount.email())
                .claim("name", userAccount.name())
                .build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthToken(tokenValue, TOKEN_TYPE, jwtProperties.accessTokenTtl().toSeconds());
    }

    private static JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        byte[] secretBytes = jwtProperties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }
}
