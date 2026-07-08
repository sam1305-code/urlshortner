package com.example.urlshortener.url.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.urlshortener.url.model.ShortUrl;

@Repository
public class InMemoryShortUrlRepository implements ShortUrlRepository {

    private final Map<String, ShortUrl> urlsByShortCode = new ConcurrentHashMap<>();

    @Override
    public boolean insertIfShortCodeAbsent(ShortUrl shortUrl) {
        return urlsByShortCode.putIfAbsent(shortUrl.shortCode(), shortUrl) == null;
    }

    @Override
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        return Optional.ofNullable(urlsByShortCode.get(shortCode));
    }
}
