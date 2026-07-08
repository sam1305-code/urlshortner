package com.example.urlshortener.url.repository;

import java.util.Optional;

import com.example.urlshortener.url.model.ShortUrl;

public interface ShortUrlRepository {

    boolean insertIfShortCodeAbsent(ShortUrl shortUrl);

    Optional<ShortUrl> findByShortCode(String shortCode);
}
