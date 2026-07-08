package com.example.urlshortener.analytics.model;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
        UUID id,
        String shortCode,
        Instant clickedAt,
        String ipAddress,
        String userAgent,
        String referer) {
}
