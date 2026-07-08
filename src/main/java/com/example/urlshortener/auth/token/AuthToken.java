package com.example.urlshortener.auth.token;

public record AuthToken(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
