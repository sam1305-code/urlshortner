package com.example.urlshortener.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;
import com.example.urlshortener.auth.model.UserAccount;

@Service
public class InMemoryAuthService implements AuthService {

    private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public InMemoryAuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        UserAccount userAccount = new UserAccount(
                UUID.randomUUID(),
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Instant.now());

        UserAccount existingUser = usersByEmail.putIfAbsent(normalizedEmail, userAccount);
        if (existingUser != null) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        return userAccount.toRegistrationResponse();
    }

    Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(normalizeEmail(email)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
