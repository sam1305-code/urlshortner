package com.example.urlshortener.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;
import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.auth.repository.UserAccountRepository;
import com.example.urlshortener.auth.util.EmailNormalizer;

@Service
public class DefaultAuthService implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        UserAccount userAccount = new UserAccount(
                UUID.randomUUID(),
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Instant.now());

        boolean inserted = userAccountRepository.insertIfEmailAbsent(userAccount);
        if (!inserted) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        return userAccount.toRegistrationResponse();
    }
}
