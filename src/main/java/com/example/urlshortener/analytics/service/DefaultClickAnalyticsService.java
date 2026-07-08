package com.example.urlshortener.analytics.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.urlshortener.analytics.dto.ClickAnalyticsResponse;
import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.repository.ShortUrlRepository;

@Service
public class DefaultClickAnalyticsService implements ClickAnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final Clock clock;

    @Autowired
    public DefaultClickAnalyticsService(
            ClickEventRepository clickEventRepository,
            ShortUrlRepository shortUrlRepository) {
        this(clickEventRepository, shortUrlRepository, Clock.systemUTC());
    }

    DefaultClickAnalyticsService(
            ClickEventRepository clickEventRepository,
            ShortUrlRepository shortUrlRepository,
            Clock clock) {
        this.clickEventRepository = clickEventRepository;
        this.shortUrlRepository = shortUrlRepository;
        this.clock = clock;
    }

    @Override
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        clickEventRepository.save(new ClickEvent(
                UUID.randomUUID(),
                shortCode,
                Instant.now(clock),
                ipAddress,
                userAgent,
                referer));
    }

    @Override
    public ClickAnalyticsResponse getAnalytics(UUID ownerId, String shortCode) {
        shortUrlRepository.findByShortCodeAndOwnerId(shortCode, ownerId)
                .filter(shortUrl -> !shortUrl.deleted())
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        return new ClickAnalyticsResponse(shortCode, clickEventRepository.countByShortCode(shortCode));
    }
}
