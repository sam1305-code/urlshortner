package com.example.urlshortener.url.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.urlshortener.analytics.dto.ClickAnalyticsResponse;
import com.example.urlshortener.analytics.service.ClickAnalyticsService;
import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.service.ShortUrlService;

class RedirectControllerTest {

    private StubShortUrlService shortUrlService;
    private StubClickAnalyticsService clickAnalyticsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shortUrlService = new StubShortUrlService();
        clickAnalyticsService = new StubClickAnalyticsService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RedirectController(shortUrlService, clickAnalyticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void redirectReturnsFoundWithLocationHeader() throws Exception {
        shortUrlService.originalUrl = "https://example.com/docs";

        mockMvc.perform(get("/abc123XY")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .header(HttpHeaders.REFERER, "https://referrer.example.com")
                        .header("X-Forwarded-For", "203.0.113.10, 198.51.100.20"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/docs"));

        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.shortCode).isEqualTo("abc123XY");
        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.ipAddress).isEqualTo("203.0.113.10");
        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.userAgent).isEqualTo("Mozilla/5.0");
        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.referer).isEqualTo("https://referrer.example.com");
    }

    @Test
    void redirectSupportsCustomAliasShape() throws Exception {
        shortUrlService.originalUrl = "https://example.com/docs";

        mockMvc.perform(get("/my-alias"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/docs"));
    }

    @Test
    void redirectReturnsNotFoundWhenShortCodeDoesNotExist() throws Exception {
        shortUrlService.notFound = true;

        mockMvc.perform(get("/abc123XY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short URL was not found."));

        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.recorded).isFalse();
    }

    @Test
    void redirectReturnsGoneWhenShortCodeExpired() throws Exception {
        shortUrlService.expired = true;

        mockMvc.perform(get("/abc123XY"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Short URL has expired."));

        org.assertj.core.api.Assertions.assertThat(clickAnalyticsService.recorded).isFalse();
    }

    private static class StubShortUrlService implements ShortUrlService {

        private String originalUrl;
        private boolean notFound;
        private boolean expired;

        @Override
        public ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }

        @Override
        public String resolveOriginalUrl(String shortCode) {
            if (notFound) {
                throw new ShortUrlNotFoundException(shortCode);
            }
            if (expired) {
                throw new ShortUrlExpiredException(shortCode);
            }
            return originalUrl;
        }
    }

    private static class StubClickAnalyticsService implements ClickAnalyticsService {

        private boolean recorded;
        private String shortCode;
        private String ipAddress;
        private String userAgent;
        private String referer;

        @Override
        public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
            this.recorded = true;
            this.shortCode = shortCode;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.referer = referer;
        }

        @Override
        public ClickAnalyticsResponse getAnalytics(UUID ownerId, String shortCode) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }
    }
}
