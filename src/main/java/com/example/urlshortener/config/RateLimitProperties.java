package com.example.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int capacity,
        int refillTokens,
        Duration refillPeriod) {
}
