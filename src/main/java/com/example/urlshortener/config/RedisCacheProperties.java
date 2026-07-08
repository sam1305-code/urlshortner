package com.example.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis-cache")
public record RedisCacheProperties(
        Duration redirectTtl) {
}
