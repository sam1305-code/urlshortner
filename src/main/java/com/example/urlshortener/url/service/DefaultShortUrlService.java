package com.example.urlshortener.url.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.url.util.ShortCodeGenerator;

@Service
public class DefaultShortUrlService implements ShortUrlService {

    private static final int MAX_SHORT_CODE_ATTEMPTS = 5;

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final Clock clock;

    @Autowired
    public DefaultShortUrlService(ShortUrlRepository shortUrlRepository, ShortCodeGenerator shortCodeGenerator) {
        this(shortUrlRepository, shortCodeGenerator, Clock.systemUTC());
    }

    DefaultShortUrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            Clock clock) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.clock = clock;
    }

    @Override
    public ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request) {
        for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
            ShortUrl shortUrl = new ShortUrl(
                    UUID.randomUUID(),
                    shortCodeGenerator.generate(),
                    request.originalUrl(),
                    ownerId,
                    Instant.now(clock),
                    request.expiresAt(),
                    false);

            if (shortUrlRepository.insertIfShortCodeAbsent(shortUrl)) {
                return shortUrl.toResponse();
            }
        }

        throw new IllegalStateException("Unable to allocate a unique short code.");
    }

    @Override
    public String resolveOriginalUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .filter(url -> !url.deleted())
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (shortUrl.expiresAt() != null && !shortUrl.expiresAt().isAfter(Instant.now(clock))) {
            throw new ShortUrlExpiredException(shortCode);
        }

        return shortUrl.originalUrl();
    }
}
