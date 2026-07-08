package com.example.urlshortener.ratelimit;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter rateLimiter;

    public RateLimitingFilter(InMemoryRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!shouldRateLimit(request) || rateLimiter.tryConsume(rateLimitKey(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequestsResponse(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        return ("POST".equals(method) && path.startsWith("/api/v1/auth/"))
                || ("POST".equals(method) && "/api/v1/urls".equals(path))
                || ("DELETE".equals(method) && path.startsWith("/api/v1/urls/"));
    }

    private String rateLimitKey(HttpServletRequest request) {
        return clientIpAddress(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
    }

    private String clientIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Too many requests. Please try again later.","path":"%s","fieldErrors":[]}
                """.formatted(Instant.now(), request.getRequestURI()).trim());
    }
}
