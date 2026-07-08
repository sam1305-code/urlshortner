package com.example.urlshortener.url.model;

import java.time.Instant;
import java.util.UUID;

import com.example.urlshortener.url.dto.ShortUrlResponse;

public record ShortUrl(
        UUID id,
        String shortCode,
        String originalUrl,
        UUID ownerId,
        Instant createdAt,
        Instant expiresAt,
        boolean deleted) {

    public ShortUrlResponse toResponse() {
        return new ShortUrlResponse(id, shortCode, originalUrl, ownerId, createdAt, expiresAt);
    }

    public ShortUrl markDeleted() {
        return new ShortUrl(id, shortCode, originalUrl, ownerId, createdAt, expiresAt, true);
    }
}
