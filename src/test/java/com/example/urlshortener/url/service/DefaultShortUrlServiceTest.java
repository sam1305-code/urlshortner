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
                null,
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

        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/one", null, null));
        ShortUrlResponse secondResponse = service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/two",
                null,
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
                null,
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
                null,
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

    @Test
    void createShortUrlUsesCustomAliasWhenProvided() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("ignored1"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");

        ShortUrlResponse response = service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                "my-alias",
                null));

        assertThat(response.shortCode()).isEqualTo("my-alias");
        assertThat(repository.findByShortCode("my-alias")).isPresent();
    }

    @Test
    void createShortUrlRejectsCustomAliasWhenAlreadyInUse() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("ignored1"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/one",
                "my-alias",
                null));

        assertThatThrownBy(() -> service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/two",
                "my-alias",
                null)))
                .isInstanceOf(com.example.urlshortener.url.exception.ShortCodeAlreadyExistsException.class);
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
