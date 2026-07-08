package com.example.urlshortener.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.repository.InMemoryShortUrlRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.url.util.ShortCodeGenerator;

class DefaultShortUrlServiceTest {

    private ShortUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryShortUrlRepository();
    }

    @Test
    void createShortUrlStoresUrlForOwner() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        Instant expiresAt = Instant.parse("2026-07-09T10:15:30Z");

        ShortUrlResponse response = service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                expiresAt));

        assertThat(response.shortCode()).isEqualTo("abc123XY");
        assertThat(response.originalUrl()).isEqualTo("https://example.com/docs");
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-07-08T10:15:30Z"));
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(repository.findByShortCode("abc123XY")).isPresent();
    }

    @Test
    void createShortUrlRetriesWhenGeneratedCodeCollides() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("sameCode", "sameCode", "nextCode"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");

        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/one", null));
        ShortUrlResponse secondResponse = service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/two",
                null));

        assertThat(secondResponse.shortCode()).isEqualTo("nextCode");
    }

    @Test
    void resolveOriginalUrlReturnsStoredUrlWhenActive() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                Instant.parse("2026-07-09T10:15:30Z")));

        String originalUrl = service.resolveOriginalUrl("abc123XY");

        assertThat(originalUrl).isEqualTo("https://example.com/docs");
    }

    @Test
    void resolveOriginalUrlRejectsExpiredUrl() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                Instant.parse("2026-07-08T10:15:30Z")));

        assertThatThrownBy(() -> service.resolveOriginalUrl("abc123XY"))
                .isInstanceOf(ShortUrlExpiredException.class);
    }

    @Test
    void resolveOriginalUrlRejectsMissingUrl() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());

        assertThatThrownBy(() -> service.resolveOriginalUrl("missing1"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneOffset.UTC);
    }

    private static class StubShortCodeGenerator implements ShortCodeGenerator {

        private final Queue<String> codes = new ArrayDeque<>();

        StubShortCodeGenerator(String... codes) {
            this.codes.addAll(java.util.List.of(codes));
        }

        @Override
        public String generate() {
            return codes.remove();
        }
    }
}
