package com.example.urlshortener.url.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.urlshortener.config.RedisCacheProperties;
import com.example.urlshortener.url.model.ShortUrl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Profile("redis")
public class RedisShortUrlCache implements ShortUrlCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisShortUrlCache.class);
    private static final String KEY_PREFIX = "short-url:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration redirectTtl;

    @Autowired
    public RedisShortUrlCache(
            StringRedisTemplate redisTemplate,
            RedisCacheProperties redisCacheProperties) {
        this(redisTemplate, new ObjectMapper(), redisCacheProperties);
    }

    RedisShortUrlCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisCacheProperties redisCacheProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redirectTtl = redisCacheProperties.redirectTtl();
    }

    @Override
    public Optional<CachedShortUrl> get(String shortCode) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(key(shortCode));
            if (cachedValue == null) {
                return Optional.empty();
            }

            RedisCachedShortUrl cachedShortUrl = objectMapper.readValue(cachedValue, RedisCachedShortUrl.class);
            return Optional.of(cachedShortUrl.toDomain());
        } catch (JsonProcessingException exception) {
            evict(shortCode);
            return Optional.empty();
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis redirect cache read failed for short code {}. Falling back to repository.", shortCode, exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(ShortUrl shortUrl, Instant now) {
        try {
            Duration ttl = effectiveTtl(shortUrl, now);
            if (!ttl.isPositive()) {
                return;
            }

            RedisCachedShortUrl cachedShortUrl = new RedisCachedShortUrl(
                    shortUrl.shortCode(),
                    shortUrl.originalUrl(),
                    shortUrl.expiresAt() == null ? null : shortUrl.expiresAt().toString());

            redisTemplate.opsForValue().set(
                    key(shortUrl.shortCode()),
                    objectMapper.writeValueAsString(cachedShortUrl),
                    ttl);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Redis redirect cache serialization failed for short code {}.", shortUrl.shortCode(), exception);
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis redirect cache write failed for short code {}.", shortUrl.shortCode(), exception);
        }
    }

    @Override
    public void evict(String shortCode) {
        try {
            redisTemplate.delete(key(shortCode));
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis redirect cache eviction failed for short code {}.", shortCode, exception);
        }
    }

    private Duration effectiveTtl(ShortUrl shortUrl, Instant now) {
        if (shortUrl.expiresAt() == null) {
            return redirectTtl;
        }

        Duration remainingLifetime = Duration.between(now, shortUrl.expiresAt());
        if (remainingLifetime.compareTo(redirectTtl) < 0) {
            return remainingLifetime;
        }
        return redirectTtl;
    }

    private String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }

    private record RedisCachedShortUrl(
            String shortCode,
            String originalUrl,
            String expiresAt) {

        private CachedShortUrl toDomain() {
            return new CachedShortUrl(
                    shortCode,
                    originalUrl,
                    expiresAt == null ? null : Instant.parse(expiresAt));
        }
    }
}
