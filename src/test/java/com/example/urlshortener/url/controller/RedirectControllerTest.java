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

import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.exception.ShortUrlExpiredException;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.service.ShortUrlService;

class RedirectControllerTest {

    private StubShortUrlService shortUrlService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shortUrlService = new StubShortUrlService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RedirectController(shortUrlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void redirectReturnsFoundWithLocationHeader() throws Exception {
        shortUrlService.originalUrl = "https://example.com/docs";

        mockMvc.perform(get("/abc123XY"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/docs"));
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
    }

    @Test
    void redirectReturnsGoneWhenShortCodeExpired() throws Exception {
        shortUrlService.expired = true;

        mockMvc.perform(get("/abc123XY"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Short URL has expired."));
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
}
