package com.example.urlshortener.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;

@Service
public class InMemoryAuthService implements AuthService {

    private final Map<String, UserRegistrationResponse> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        UserRegistrationResponse response = new UserRegistrationResponse(
                UUID.randomUUID(),
                request.name().trim(),
                normalizedEmail,
                Instant.now());

        UserRegistrationResponse existingUser = usersByEmail.putIfAbsent(normalizedEmail, response);
        if (existingUser != null) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        return response;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
