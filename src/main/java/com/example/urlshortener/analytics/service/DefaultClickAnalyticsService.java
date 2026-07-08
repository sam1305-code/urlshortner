package com.example.urlshortener.analytics.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.analytics.repository.ClickEventRepository;

@Service
public class DefaultClickAnalyticsService implements ClickAnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final Clock clock;

    @Autowired
    public DefaultClickAnalyticsService(ClickEventRepository clickEventRepository) {
        this(clickEventRepository, Clock.systemUTC());
    }

    DefaultClickAnalyticsService(ClickEventRepository clickEventRepository, Clock clock) {
        this.clickEventRepository = clickEventRepository;
        this.clock = clock;
    }

    @Override
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        clickEventRepository.save(new ClickEvent(
                UUID.randomUUID(),
                shortCode,
                Instant.now(clock),
                ipAddress,
                userAgent,
                referer));
    }
}
