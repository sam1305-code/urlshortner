package com.example.urlshortener.url.cache;

import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.urlshortener.url.model.ShortUrl;

@Component
@Profile("!redis")
public class NoOpShortUrlCache implements ShortUrlCache {

    @Override
    public Optional<CachedShortUrl> get(String shortCode) {
        return Optional.empty();
    }

    @Override
    public void put(ShortUrl shortUrl, Instant now) {
        // Intentionally empty: local/default profile should not require Redis.
    }

    @Override
    public void evict(String shortCode) {
        // Intentionally empty: local/default profile should not require Redis.
    }
}
