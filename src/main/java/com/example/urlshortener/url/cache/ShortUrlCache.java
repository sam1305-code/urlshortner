package com.example.urlshortener.url.cache;

import java.time.Instant;
import java.util.Optional;

import com.example.urlshortener.url.model.ShortUrl;

public interface ShortUrlCache {

    Optional<CachedShortUrl> get(String shortCode);

    void put(ShortUrl shortUrl, Instant now);

    void evict(String shortCode);
}
