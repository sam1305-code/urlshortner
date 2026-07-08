package com.example.urlshortener.url.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.service.ShortUrlService;

class ShortUrlControllerTest {

    private StubShortUrlService shortUrlService;
    private ShortUrlController controller;

    @BeforeEach
    void setUp() {
        shortUrlService = new StubShortUrlService();
        controller = new ShortUrlController(shortUrlService);
    }

    @Test
    void createShortUrlReturnsCreatedResponseForAuthenticatedOwner() {
        UUID ownerId = UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f");
        shortUrlService.response = new ShortUrlResponse(
                UUID.fromString("48d44fb6-bb15-4882-8786-12f536439cb0"),
                "abc123XY",
                "https://example.com/docs",
                ownerId,
                Instant.parse("2026-07-08T10:15:30Z"),
                Instant.parse("2026-07-09T10:15:30Z"));

        ResponseEntity<ShortUrlResponse> response = controller.createShortUrl(jwt(ownerId), new CreateShortUrlRequest(
                "https://example.com/docs",
                Instant.parse("2026-07-09T10:15:30Z")));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/urls/abc123XY");
        assertThat(response.getBody()).isEqualTo(shortUrlService.response);
        assertThat(shortUrlService.ownerId).isEqualTo(ownerId);
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .build();
    }

    private static class StubShortUrlService implements ShortUrlService {

        private UUID ownerId;
        private ShortUrlResponse response;

        @Override
        public ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request) {
            this.ownerId = ownerId;
            return response;
        }
    }
}
