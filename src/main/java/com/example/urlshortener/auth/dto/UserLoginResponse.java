package com.example.urlshortener.auth.dto;

import java.util.UUID;

public record UserLoginResponse(
        UUID userId,
        String name,
        String email) {
}
