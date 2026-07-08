package com.example.urlshortener.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.analytics.repository.InMemoryClickEventRepository;

class DefaultClickAnalyticsServiceTest {

    private ClickEventRepository clickEventRepository;
    private DefaultClickAnalyticsService clickAnalyticsService;

    @BeforeEach
    void setUp() {
        clickEventRepository = new InMemoryClickEventRepository();
        clickAnalyticsService = new DefaultClickAnalyticsService(
                clickEventRepository,
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
}
