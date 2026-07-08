package com.example.urlshortener.url.dto;

import java.time.Instant;
import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String shortCode,
        String originalUrl,
        UUID ownerId,
        Instant createdAt,
        Instant expiresAt) {
}
