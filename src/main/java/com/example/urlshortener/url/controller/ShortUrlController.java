package com.example.urlshortener.url.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.analytics.dto.ClickAnalyticsResponse;
import com.example.urlshortener.analytics.service.ClickAnalyticsService;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.service.ShortUrlService;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final ClickAnalyticsService clickAnalyticsService;

    public ShortUrlController(ShortUrlService shortUrlService, ClickAnalyticsService clickAnalyticsService) {
        this.shortUrlService = shortUrlService;
        this.clickAnalyticsService = clickAnalyticsService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> createShortUrl(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = shortUrlService.createShortUrl(UUID.fromString(jwt.getSubject()), request);
        URI location = URI.create("/api/v1/urls/" + response.shortCode());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<ClickAnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String shortCode) {
        ClickAnalyticsResponse response = clickAnalyticsService.getAnalytics(UUID.fromString(jwt.getSubject()), shortCode);

        return ResponseEntity.ok(response);
    }
}
