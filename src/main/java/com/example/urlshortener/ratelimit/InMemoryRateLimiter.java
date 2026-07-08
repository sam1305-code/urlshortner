package com.example.urlshortener.ratelimit;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.urlshortener.config.RateLimitProperties;

@Component
public class InMemoryRateLimiter {

    private final Map<String, TokenBucket> bucketsByKey = new ConcurrentHashMap<>();
    private final RateLimitProperties rateLimitProperties;
    private final Clock clock;

    @Autowired
    public InMemoryRateLimiter(RateLimitProperties rateLimitProperties) {
        this(rateLimitProperties, Clock.systemUTC());
    }

    InMemoryRateLimiter(RateLimitProperties rateLimitProperties, Clock clock) {
        this.rateLimitProperties = rateLimitProperties;
        this.clock = clock;
    }

    public boolean tryConsume(String key) {
        if (!rateLimitProperties.enabled()) {
            return true;
        }

        TokenBucket bucket = bucketsByKey.computeIfAbsent(key, ignored -> new TokenBucket(
                rateLimitProperties.capacity(),
                rateLimitProperties.refillTokens(),
                rateLimitProperties.refillPeriod(),
                clock));

        return bucket.tryConsume();
    }
}
