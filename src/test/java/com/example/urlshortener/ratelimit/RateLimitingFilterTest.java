package com.example.urlshortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.urlshortener.config.RateLimitProperties;

class RateLimitingFilterTest {

    @Test
    void doFilterLimitsConfiguredEndpoints() throws Exception {
        RateLimitingFilter filter = filterWithCapacity(1);
        MockHttpServletRequest firstRequest = request("POST", "/api/v1/urls");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletRequest secondRequest = request("POST", "/api/v1/urls");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(secondResponse.getContentAsString()).contains("Too many requests");
    }

    @Test
    void doFilterDoesNotLimitUnconfiguredEndpoints() throws Exception {
        RateLimitingFilter filter = filterWithCapacity(0);
        MockHttpServletRequest request = request("GET", "/api/v1/system/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterUsesFirstForwardedIpAddressAsClientKey() throws Exception {
        RateLimitingFilter filter = filterWithCapacity(1);
        MockHttpServletRequest firstRequest = request("POST", "/api/v1/auth/login");
        firstRequest.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        MockHttpServletRequest secondRequest = request("POST", "/api/v1/auth/login");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");

        filter.doFilter(firstRequest, new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(secondResponse.getStatus()).isEqualTo(429);
    }

    private RateLimitingFilter filterWithCapacity(int capacity) {
        var rateLimiter = new InMemoryRateLimiter(
                new RateLimitProperties(true, capacity, capacity, Duration.ofMinutes(1)),
                Clock.fixed(Instant.parse("2026-07-08T10:15:30Z"), ZoneId.of("UTC")));
        return new RateLimitingFilter(rateLimiter);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
