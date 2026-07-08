package com.example.urlshortener.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;
import com.example.urlshortener.auth.model.UserAccount;

class InMemoryAuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private InMemoryAuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        authService = new InMemoryAuthService(passwordEncoder);
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        authService.register(new UserRegistrationRequest(
                "  Samhita  ",
                "  SAMHITA@example.COM  ",
                "StrongPass123!"));

        UserAccount userAccount = authService.findByEmail("samhita@example.com").orElseThrow();

        assertThat(userAccount.name()).isEqualTo("Samhita");
        assertThat(userAccount.email()).isEqualTo("samhita@example.com");
        assertThat(userAccount.passwordHash()).isNotEqualTo("StrongPass123!");
        assertThat(passwordEncoder.matches("StrongPass123!", userAccount.passwordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCaseAndWhitespace() {
        authService.register(new UserRegistrationRequest(
                "Samhita",
                "samhita@example.com",
                "StrongPass123!"));

        UserRegistrationRequest duplicateRequest = new UserRegistrationRequest(
                "Samhita Again",
                "  SAMHITA@example.com ",
                "AnotherStrong123!");

        assertThatThrownBy(() -> authService.register(duplicateRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
