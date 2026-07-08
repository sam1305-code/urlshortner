package com.example.urlshortener.user.dto;

import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String name,
        String email) {
}
