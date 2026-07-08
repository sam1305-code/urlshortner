package com.example.urlshortener.analytics.service;

public interface ClickAnalyticsService {

    void recordClick(String shortCode, String ipAddress, String userAgent, String referer);
}
