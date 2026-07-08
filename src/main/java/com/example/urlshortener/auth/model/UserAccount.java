package com.example.urlshortener.auth.model;

import java.time.Instant;
import java.util.UUID;

import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.dto.UserLoginResponse;

public record UserAccount(
        UUID id,
        String name,
        String email,
        String passwordHash,
        Instant createdAt) {

    public UserRegistrationResponse toRegistrationResponse() {
        return new UserRegistrationResponse(id, name, email, createdAt);
    }

    public UserLoginResponse toLoginResponse() {
        return new UserLoginResponse(id, name, email);
    }
}
