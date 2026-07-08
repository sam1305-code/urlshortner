package com.example.urlshortener.url.service;

import java.util.UUID;

import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;

public interface ShortUrlService {

    ShortUrlResponse createShortUrl(UUID ownerId, CreateShortUrlRequest request);

    String resolveOriginalUrl(String shortCode);
}
