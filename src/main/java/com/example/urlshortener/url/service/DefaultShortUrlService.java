package com.example.urlshortener.url.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.PagedShortUrlResponse;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.cache.NoOpShortUrlCache;
import com.example.urlshortener.url.cache.ShortUrlCache;
import com.example.urlshortener.url.exception.InvalidPageRequestException;
import com.example.urlshortener.url.exception.ShortCodeAlreadyExistsException;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.url.util.ShortCodeGenerator;

@Service
public class DefaultShortUrlService implements ShortUrlService {

    private static final int MAX_SHORT_CODE_ATTEMPTS = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlCache shortUrlCache;
    private final Clock clock;

    @Autowired
    public DefaultShortUrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            ShortUrlCache shortUrlCache) {
        this(shortUrlRepository, shortCodeGenerator, shortUrlCache, Clock.systemUTC());
    }

    DefaultShortUrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            Clock clock) {
        this(shortUrlRepository, shortCodeGenerator, new NoOpShortUrlCache(), clock);
    }

    DefaultShortUrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            ShortUrlCache shortUrlCache,
            Clock clock) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.shortUrlCache = shortUrlCache;
        this.clock = clock;
    }

    @Override
    public ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request) {
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            return createWithShortCode(ownerId, request, request.customAlias());
        }

        for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
            String generatedShortCode = shortCodeGenerator.generate();
            ShortUrl shortUrl = buildShortUrl(ownerId, request, generatedShortCode);

            if (insert(shortUrl)) {
                return shortUrl.toResponse();
            }
        }

        throw new IllegalStateException("Unable to allocate a unique short code.");
    }

    @Override
    public String resolveOriginalUrl(String shortCode) {
        Instant now = Instant.now(clock);
        var cachedShortUrl = shortUrlCache.get(shortCode);
        if (cachedShortUrl.isPresent()) {
            if (!cachedShortUrl.orElseThrow().isExpired(now)) {
                return cachedShortUrl.orElseThrow().originalUrl();
            }
            shortUrlCache.evict(shortCode);
        }

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .filter(url -> !url.deleted())
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (shortUrl.expiresAt() != null && !shortUrl.expiresAt().isAfter(now)) {
            throw new ShortUrlExpiredException(shortCode);
        }

        shortUrlCache.put(shortUrl, now);
        return shortUrl.originalUrl();
    }

    @Override
    public PagedShortUrlResponse listShortUrls(UUID ownerId, String searchTerm, int page, int size) {
        validatePageRequest(page, size);

        List<ShortUrlResponse> allMatches = shortUrlRepository.findActiveByOwnerId(ownerId, searchTerm)
                .stream()
                .map(ShortUrl::toResponse)
                .toList();
        int totalItems = allMatches.size();
        int fromIndex = Math.min(page * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);

        return new PagedShortUrlResponse(
                allMatches.subList(fromIndex, toIndex),
                page,
                size,
                totalItems,
                totalPages);
    }

    @Override
    public void deleteShortUrl(UUID ownerId, String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCodeAndOwnerId(shortCode, ownerId)
                .filter(url -> !url.deleted())
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        shortUrlRepository.save(shortUrl.markDeleted());
        shortUrlCache.evict(shortCode);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("Page index must be zero or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException("Page size must be between 1 and 100.");
        }
    }

    private ShortUrlResponse createWithShortCode(UUID ownerId, CreateShortUrlRequest request, String shortCode) {
        ShortUrl shortUrl = buildShortUrl(ownerId, request, shortCode);
        if (!insert(shortUrl)) {
            throw new ShortCodeAlreadyExistsException(shortCode);
        }

        return shortUrl.toResponse();
    }

    private ShortUrl buildShortUrl(UUID ownerId, CreateShortUrlRequest request, String shortCode) {
        return new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                request.originalUrl(),
                ownerId,
                Instant.now(clock),
                request.expiresAt(),
                false);
    }

    private boolean insert(ShortUrl shortUrl) {
        return shortUrlRepository.insertIfShortCodeAbsent(shortUrl);
    }
}
