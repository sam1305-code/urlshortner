package com.example.urlshortener.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt) {
}
