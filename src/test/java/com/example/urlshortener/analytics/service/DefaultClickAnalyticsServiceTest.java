package com.example.urlshortener.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.analytics.repository.InMemoryClickEventRepository;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.InMemoryShortUrlRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;

class DefaultClickAnalyticsServiceTest {

    private ClickEventRepository clickEventRepository;
    private ShortUrlRepository shortUrlRepository;
    private DefaultClickAnalyticsService clickAnalyticsService;

    @BeforeEach
    void setUp() {
        clickEventRepository = new InMemoryClickEventRepository();
        shortUrlRepository = new InMemoryShortUrlRepository();
        clickAnalyticsService = new DefaultClickAnalyticsService(
                clickEventRepository,
                shortUrlRepository,
                Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneOffset.UTC));
    }

    @Test
    void recordClickStoresClickEvent() {
        clickAnalyticsService.recordClick(
                "abc123XY",
                "203.0.113.10",
                "Mozilla/5.0",
                "https://referrer.example.com");

        ClickEvent clickEvent = clickEventRepository.findByShortCode("abc123XY").getFirst();

        assertThat(clickEvent.shortCode()).isEqualTo("abc123XY");
        assertThat(clickEvent.clickedAt()).isEqualTo(Instant.parse("2026-07-08T10:15:30Z"));
        assertThat(clickEvent.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(clickEvent.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(clickEvent.referer()).isEqualTo("https://referrer.example.com");
    }

    @Test
    void getAnalyticsReturnsClickCountForUrlOwner() {
        UUID ownerId = UUID.fromString("4c2b0f9d-87ef-4199-a75b-1c8d85c0774a");
        shortUrlRepository.insertIfShortCodeAbsent(shortUrl("abc123XY", ownerId, false));
        clickAnalyticsService.recordClick("abc123XY", "203.0.113.10", "Mozilla/5.0", null);
        clickAnalyticsService.recordClick("abc123XY", "203.0.113.11", "curl/8.0", null);
        clickAnalyticsService.recordClick("other123", "203.0.113.12", "curl/8.0", null);

        var response = clickAnalyticsService.getAnalytics(ownerId, "abc123XY");

        assertThat(response.shortCode()).isEqualTo("abc123XY");
        assertThat(response.totalClicks()).isEqualTo(2);
    }

    @Test
    void getAnalyticsRejectsUrlsOwnedByAnotherUser() {
        UUID ownerId = UUID.fromString("4c2b0f9d-87ef-4199-a75b-1c8d85c0774a");
        UUID otherOwnerId = UUID.fromString("96e36d43-c7c4-45d7-bd31-18aef918c5b7");
        shortUrlRepository.insertIfShortCodeAbsent(shortUrl("abc123XY", otherOwnerId, false));

        assertThatThrownBy(() -> clickAnalyticsService.getAnalytics(ownerId, "abc123XY"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    private ShortUrl shortUrl(String shortCode, UUID ownerId, boolean deleted) {
        return new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                "https://example.com/docs",
                ownerId,
                Instant.parse("2026-07-08T10:15:30Z"),
                null,
                deleted);
    }
}
