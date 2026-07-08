package com.example.urlshortener.url.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.urlshortener.analytics.dto.ClickAnalyticsResponse;
import com.example.urlshortener.analytics.service.ClickAnalyticsService;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.PagedShortUrlResponse;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.service.ShortUrlService;

class ShortUrlControllerTest {

    private StubShortUrlService shortUrlService;
    private StubClickAnalyticsService clickAnalyticsService;
    private ShortUrlController controller;

    @BeforeEach
    void setUp() {
        shortUrlService = new StubShortUrlService();
        clickAnalyticsService = new StubClickAnalyticsService();
        controller = new ShortUrlController(shortUrlService, clickAnalyticsService);
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
                null,
                Instant.parse("2026-07-09T10:15:30Z")));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/urls/abc123XY");
        assertThat(response.getBody()).isEqualTo(shortUrlService.response);
        assertThat(shortUrlService.ownerId).isEqualTo(ownerId);
    }

    @Test
    void getAnalyticsReturnsAnalyticsForAuthenticatedOwner() {
        UUID ownerId = UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f");
        clickAnalyticsService.response = new ClickAnalyticsResponse("abc123XY", 42);

        ResponseEntity<ClickAnalyticsResponse> response = controller.getAnalytics(jwt(ownerId), "abc123XY");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(clickAnalyticsService.response);
        assertThat(clickAnalyticsService.ownerId).isEqualTo(ownerId);
        assertThat(clickAnalyticsService.shortCode).isEqualTo("abc123XY");
    }

    @Test
    void listShortUrlsReturnsPageForAuthenticatedOwner() {
        UUID ownerId = UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f");
        shortUrlService.pageResponse = new PagedShortUrlResponse(java.util.List.of(), 1, 10, 0, 0);

        ResponseEntity<PagedShortUrlResponse> response = controller.listShortUrls(jwt(ownerId), "docs", 1, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(shortUrlService.pageResponse);
        assertThat(shortUrlService.ownerId).isEqualTo(ownerId);
        assertThat(shortUrlService.searchTerm).isEqualTo("docs");
        assertThat(shortUrlService.page).isEqualTo(1);
        assertThat(shortUrlService.size).isEqualTo(10);
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
        private String searchTerm;
        private int page;
        private int size;
        private PagedShortUrlResponse pageResponse;

        @Override
        public ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request) {
            this.ownerId = ownerId;
            return response;
        }

        @Override
        public String resolveOriginalUrl(String shortCode) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }

        @Override
        public PagedShortUrlResponse listShortUrls(UUID ownerId, String searchTerm, int page, int size) {
            this.ownerId = ownerId;
            this.searchTerm = searchTerm;
            this.page = page;
            this.size = size;
            return pageResponse;
        }
    }

    private static class StubClickAnalyticsService implements ClickAnalyticsService {

        private UUID ownerId;
        private String shortCode;
        private ClickAnalyticsResponse response;

        @Override
        public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }

        @Override
        public ClickAnalyticsResponse getAnalytics(UUID ownerId, String shortCode) {
            this.ownerId = ownerId;
            this.shortCode = shortCode;
            return response;
        }
    }
}
