package com.example.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.url-shortener")
public record UrlShortenerProperties(
        String publicBaseUrl) {
}
