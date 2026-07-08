package com.example.urlshortener.url.cache;

import java.time.Instant;

public record CachedShortUrl(
        String shortCode,
        String originalUrl,
        Instant expiresAt) {

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
