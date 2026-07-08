package com.example.urlshortener.url.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.urlshortener.url.model.ShortUrl;

@Repository
@Profile("!couchbase")
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

    @Override
    public Optional<ShortUrl> findByShortCodeAndOwnerId(String shortCode, UUID ownerId) {
        return findByShortCode(shortCode)
                .filter(shortUrl -> shortUrl.ownerId().equals(ownerId));
    }

    @Override
    public List<ShortUrl> findActiveByOwnerId(UUID ownerId, String searchTerm) {
        String normalizedSearchTerm = normalize(searchTerm);

        return urlsByShortCode.values()
                .stream()
                .filter(shortUrl -> shortUrl.ownerId().equals(ownerId))
                .filter(shortUrl -> !shortUrl.deleted())
                .filter(shortUrl -> matchesSearch(shortUrl, normalizedSearchTerm))
                .sorted(Comparator.comparing(ShortUrl::createdAt).reversed())
                .toList();
    }

    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        urlsByShortCode.put(shortUrl.shortCode(), shortUrl);
        return shortUrl;
    }

    private boolean matchesSearch(ShortUrl shortUrl, String normalizedSearchTerm) {
        if (normalizedSearchTerm == null) {
            return true;
        }

        return shortUrl.shortCode().toLowerCase(Locale.ROOT).contains(normalizedSearchTerm)
                || shortUrl.originalUrl().toLowerCase(Locale.ROOT).contains(normalizedSearchTerm);
    }

    private String normalize(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return null;
        }

        return searchTerm.trim().toLowerCase(Locale.ROOT);
    }
}
