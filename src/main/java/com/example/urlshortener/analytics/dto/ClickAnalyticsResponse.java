package com.example.urlshortener.analytics.dto;

public record ClickAnalyticsResponse(
        String shortCode,
        long totalClicks) {
}
