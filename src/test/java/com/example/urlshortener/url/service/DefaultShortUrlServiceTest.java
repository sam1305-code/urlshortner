package com.example.urlshortener.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.url.cache.CachedShortUrl;
import com.example.urlshortener.url.cache.ShortUrlCache;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.exception.InvalidPageRequestException;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.model.ShortUrl;
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
    void resolveOriginalUrlReturnsCachedUrlWithoutRepositoryLookup() {
        RecordingShortUrlCache cache = new RecordingShortUrlCache();
        cache.cachedShortUrls.put("abc123XY", new CachedShortUrl(
                "abc123XY",
                "https://cached.example.com/docs",
                Instant.parse("2026-07-09T10:15:30Z")));
        DefaultShortUrlService service = new DefaultShortUrlService(
                new FailingShortUrlRepository(),
                new StubShortCodeGenerator("unused01"),
                cache,
                fixedClock());

        String originalUrl = service.resolveOriginalUrl("abc123XY");

        assertThat(originalUrl).isEqualTo("https://cached.example.com/docs");
        assertThat(cache.getCalls).isEqualTo(1);
    }

    @Test
    void resolveOriginalUrlPopulatesCacheAfterRepositoryLookup() {
        RecordingShortUrlCache cache = new RecordingShortUrlCache();
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                cache,
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                null,
                Instant.parse("2026-07-09T10:15:30Z")));

        String originalUrl = service.resolveOriginalUrl("abc123XY");

        assertThat(originalUrl).isEqualTo("https://example.com/docs");
        assertThat(cache.cachedShortUrls.get("abc123XY").originalUrl()).isEqualTo("https://example.com/docs");
        assertThat(cache.putCalls).isEqualTo(1);
    }

    @Test
    void resolveOriginalUrlEvictsExpiredCachedUrlAndRejectsExpiredRepositoryUrl() {
        RecordingShortUrlCache cache = new RecordingShortUrlCache();
        cache.cachedShortUrls.put("abc123XY", new CachedShortUrl(
                "abc123XY",
                "https://cached.example.com/docs",
                Instant.parse("2026-07-08T10:15:30Z")));
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                cache,
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest(
                "https://example.com/docs",
                null,
                Instant.parse("2026-07-08T10:15:30Z")));

        assertThatThrownBy(() -> service.resolveOriginalUrl("abc123XY"))
                .isInstanceOf(ShortUrlExpiredException.class);
        assertThat(cache.evictedShortCodes).containsEntry("abc123XY", 1);
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

    @Test
    void listShortUrlsReturnsOnlyMatchingUrlsForOwnerWithPagination() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("docs-api", "blog-api", "other01"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        UUID otherOwnerId = UUID.fromString("9eb309ac-ff10-4bc8-8f05-c778c1f8fbd3");
        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/docs", null, null));
        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/blog", null, null));
        service.createShortUrl(otherOwnerId, new CreateShortUrlRequest("https://example.com/docs", null, null));

        var response = service.listShortUrls(ownerId, "api", 0, 1);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().ownerId()).isEqualTo(ownerId);
        assertThat(response.items().getFirst().shortCode()).contains("api");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void listShortUrlsRejectsInvalidPageRequest() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());

        assertThatThrownBy(() -> service.listShortUrls(UUID.randomUUID(), null, -1, 20))
                .isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> service.listShortUrls(UUID.randomUUID(), null, 0, 101))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    void deleteShortUrlMarksUrlDeletedAndStopsPublicResolution() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/docs", null, null));

        service.deleteShortUrl(ownerId, "abc123XY");

        assertThat(repository.findByShortCode("abc123XY")).isPresent();
        assertThat(repository.findByShortCode("abc123XY").orElseThrow().deleted()).isTrue();
        assertThatThrownBy(() -> service.resolveOriginalUrl("abc123XY"))
                .isInstanceOf(ShortUrlNotFoundException.class);
        assertThat(service.listShortUrls(ownerId, null, 0, 20).items()).isEmpty();
    }

    @Test
    void deleteShortUrlEvictsCachedRedirect() {
        RecordingShortUrlCache cache = new RecordingShortUrlCache();
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                cache,
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        service.createShortUrl(ownerId, new CreateShortUrlRequest("https://example.com/docs", null, null));
        service.resolveOriginalUrl("abc123XY");

        service.deleteShortUrl(ownerId, "abc123XY");

        assertThat(cache.evictedShortCodes).containsEntry("abc123XY", 1);
    }

    @Test
    void deleteShortUrlRejectsUrlsOwnedByAnotherUser() {
        DefaultShortUrlService service = new DefaultShortUrlService(
                repository,
                new StubShortCodeGenerator("abc123XY"),
                fixedClock());
        UUID ownerId = UUID.fromString("f6042fe8-b24d-40c9-8e8a-d773752d127f");
        UUID otherOwnerId = UUID.fromString("9eb309ac-ff10-4bc8-8f05-c778c1f8fbd3");
        service.createShortUrl(otherOwnerId, new CreateShortUrlRequest("https://example.com/docs", null, null));

        assertThatThrownBy(() -> service.deleteShortUrl(ownerId, "abc123XY"))
                .isInstanceOf(ShortUrlNotFoundException.class);
        assertThat(repository.findByShortCode("abc123XY").orElseThrow().deleted()).isFalse();
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

    private static class RecordingShortUrlCache implements ShortUrlCache {

        private final Map<String, CachedShortUrl> cachedShortUrls = new HashMap<>();
        private final Map<String, Integer> evictedShortCodes = new HashMap<>();
        private int getCalls;
        private int putCalls;

        @Override
        public Optional<CachedShortUrl> get(String shortCode) {
            getCalls++;
            return Optional.ofNullable(cachedShortUrls.get(shortCode));
        }

        @Override
        public void put(ShortUrl shortUrl, Instant now) {
            putCalls++;
            cachedShortUrls.put(shortUrl.shortCode(), new CachedShortUrl(
                    shortUrl.shortCode(),
                    shortUrl.originalUrl(),
                    shortUrl.expiresAt()));
        }

        @Override
        public void evict(String shortCode) {
            cachedShortUrls.remove(shortCode);
            evictedShortCodes.merge(shortCode, 1, Integer::sum);
        }
    }

    private static class FailingShortUrlRepository extends InMemoryShortUrlRepository {

        @Override
        public Optional<ShortUrl> findByShortCode(String shortCode) {
            throw new AssertionError("Repository should not be called when redirect cache hits.");
        }
    }
}
