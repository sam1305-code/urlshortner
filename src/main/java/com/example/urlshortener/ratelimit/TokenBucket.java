package com.example.urlshortener.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

final class TokenBucket {

    private final int capacity;
    private final int refillTokens;
    private final Duration refillPeriod;
    private final Clock clock;

    private int tokens;
    private Instant lastRefillAt;

    TokenBucket(int capacity, int refillTokens, Duration refillPeriod, Clock clock) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = refillPeriod;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillAt = Instant.now(clock);
    }

    synchronized boolean tryConsume() {
        refill();
        if (tokens == 0) {
            return false;
        }

        tokens--;
        return true;
    }

    private void refill() {
        Instant now = Instant.now(clock);
        long elapsedPeriods = Duration.between(lastRefillAt, now).dividedBy(refillPeriod);
        if (elapsedPeriods <= 0) {
            return;
        }

        long tokensToAdd = elapsedPeriods * refillTokens;
        tokens = (int) Math.min(capacity, tokens + tokensToAdd);
        lastRefillAt = lastRefillAt.plus(refillPeriod.multipliedBy(elapsedPeriods));
    }
}
