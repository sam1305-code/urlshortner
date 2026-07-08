package com.example.urlshortener.analytics.service;

import java.util.UUID;

import com.example.urlshortener.analytics.dto.ClickAnalyticsResponse;

public interface ClickAnalyticsService {

    void recordClick(String shortCode, String ipAddress, String userAgent, String referer);

    ClickAnalyticsResponse getAnalytics(UUID ownerId, String shortCode);
}
