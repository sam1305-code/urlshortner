package com.example.urlshortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.example.urlshortener.config.RateLimitProperties;

class InMemoryRateLimiterTest {

    @Test
    void tryConsumeAllowsRequestsWhileTokensRemain() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, 2, 2, Duration.ofMinutes(1)),
                Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneId.of("UTC")));

        assertThat(rateLimiter.tryConsume("client-a")).isTrue();
        assertThat(rateLimiter.tryConsume("client-a")).isTrue();
        assertThat(rateLimiter.tryConsume("client-a")).isFalse();
    }

    @Test
    void tryConsumeRefillsTokensAfterRefillPeriod() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-08T10:15:30Z"));
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, 1, 1, Duration.ofMinutes(1)),
                clock);
        rateLimiter.tryConsume("client-a");

        clock.advance(Duration.ofMinutes(1));

        assertThat(rateLimiter.tryConsume("client-a")).isTrue();
    }

    @Test
    void tryConsumeIsIsolatedByKey() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, 1, 1, Duration.ofMinutes(1)),
                Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneId.of("UTC")));

        assertThat(rateLimiter.tryConsume("client-a")).isTrue();
        assertThat(rateLimiter.tryConsume("client-b")).isTrue();
        assertThat(rateLimiter.tryConsume("client-a")).isFalse();
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
