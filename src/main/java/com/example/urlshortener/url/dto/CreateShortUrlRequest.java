package com.example.urlshortener.url.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;

public record CreateShortUrlRequest(
        @NotBlank(message = "Original URL is required.")
        @Size(max = 2048, message = "Original URL must be at most 2048 characters.")
        @URL(message = "Original URL must be a valid URL.")
        @Pattern(regexp = "^https?://.*$", message = "Original URL must use HTTP or HTTPS.")
        String originalUrl,

        @Size(min = 4, max = 32, message = "Custom alias must be between 4 and 32 characters.")
        @Pattern(
                regexp = "^[0-9A-Za-z][0-9A-Za-z_-]*$",
                message = "Custom alias may contain letters, numbers, underscores, and hyphens, and must start with a letter or number.")
        String customAlias,

        @Future(message = "Expiration time must be in the future.")
        Instant expiresAt) {
}
