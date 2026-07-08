package com.example.urlshortener.url.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.urlshortener.config.RedisCacheProperties;
import com.example.urlshortener.url.model.ShortUrl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RedisShortUrlCacheTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisShortUrlCache cache;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache = new RedisShortUrlCache(
                redisTemplate,
                new ObjectMapper(),
                new RedisCacheProperties(Duration.ofMinutes(10)));
    }

    @Test
    void getReturnsCachedShortUrlWhenJsonIsValid() {
        when(valueOperations.get("short-url:abc123XY")).thenReturn("""
                {"shortCode":"abc123XY","originalUrl":"https://example.com/docs","expiresAt":"2026-07-09T10:15:30Z"}
                """);
        CachedShortUrl expectedCachedShortUrl = new CachedShortUrl(
                "abc123XY",
                "https://example.com/docs",
                Instant.parse("2026-07-09T10:15:30Z"));

        Optional<CachedShortUrl> cachedShortUrl = cache.get("abc123XY");

        assertThat(cachedShortUrl).contains(expectedCachedShortUrl);
    }

    @Test
    void getEvictsEntryWhenJsonIsInvalid() {
        when(valueOperations.get("short-url:abc123XY")).thenReturn("not-json");

        Optional<CachedShortUrl> cachedShortUrl = cache.get("abc123XY");

        assertThat(cachedShortUrl).isEmpty();
        verify(redisTemplate).delete("short-url:abc123XY");
    }

    @Test
    void getReturnsEmptyWhenRedisReadFails() {
        when(valueOperations.get("short-url:abc123XY")).thenThrow(new RuntimeException("Redis unavailable"));

        Optional<CachedShortUrl> cachedShortUrl = cache.get("abc123XY");

        assertThat(cachedShortUrl).isEmpty();
    }

    @Test
    void putUsesConfiguredTtlWhenUrlDoesNotExpire() {
        cache.put(shortUrl(null), Instant.parse("2026-07-08T10:15:30Z"));

        verify(valueOperations).set(
                eq("short-url:abc123XY"),
                eq("{\"shortCode\":\"abc123XY\",\"originalUrl\":\"https://example.com/docs\",\"expiresAt\":null}"),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void putCapsTtlAtRemainingUrlLifetime() {
        Instant expiresAt = Instant.parse("2026-07-08T10:20:30Z");
        cache.put(shortUrl(expiresAt), Instant.parse("2026-07-08T10:15:30Z"));

        verify(valueOperations).set(
                eq("short-url:abc123XY"),
                eq("{\"shortCode\":\"abc123XY\",\"originalUrl\":\"https://example.com/docs\",\"expiresAt\":\"2026-07-08T10:20:30Z\"}"),
                eq(Duration.ofMinutes(5)));
    }

    @Test
    void putDoesNotThrowWhenRedisWriteFails() {
        doThrow(new RuntimeException("Redis unavailable"))
                .when(valueOperations)
                .set(any(), any(), any(Duration.class));

        assertThatCode(() -> cache.put(shortUrl(null), Instant.parse("2026-07-08T10:15:30Z")))
                .doesNotThrowAnyException();
    }

    @Test
    void putDoesNotThrowWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {
                });
        RedisShortUrlCache cacheWithFailingMapper = new RedisShortUrlCache(
                redisTemplate,
                objectMapper,
                new RedisCacheProperties(Duration.ofMinutes(10)));

        assertThatCode(() -> cacheWithFailingMapper.put(shortUrl(null), Instant.parse("2026-07-08T10:15:30Z")))
                .doesNotThrowAnyException();
    }

    @Test
    void evictDeletesRedisKey() {
        cache.evict("abc123XY");

        verify(redisTemplate).delete("short-url:abc123XY");
    }

    @Test
    void evictDoesNotThrowWhenRedisDeleteFails() {
        when(redisTemplate.delete("short-url:abc123XY")).thenThrow(new RuntimeException("Redis unavailable"));

        assertThatCode(() -> cache.evict("abc123XY"))
                .doesNotThrowAnyException();
    }

    private ShortUrl shortUrl(Instant expiresAt) {
        return new ShortUrl(
                UUID.fromString("48d44fb6-bb15-4882-8786-12f536439cb0"),
                "abc123XY",
                "https://example.com/docs",
                UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f"),
                Instant.parse("2026-07-08T10:15:30Z"),
                expiresAt,
                false);
    }
}
