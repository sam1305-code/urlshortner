package com.example.urlshortener.url.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;

public record CreateShortUrlRequest(
        @NotBlank(message = "Original URL is required.")
        @Size(max = 2048, message = "Original URL must be at most 2048 characters.")
        @URL(protocol = "http", message = "Original URL must be a valid HTTP or HTTPS URL.")
        String originalUrl,

        @Future(message = "Expiration time must be in the future.")
        Instant expiresAt) {
}
