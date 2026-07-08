package com.example.urlshortener.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.urlshortener.auth.dto.UserLoginRequest;
import com.example.urlshortener.auth.dto.UserLoginResponse;
import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;
import com.example.urlshortener.auth.exception.InvalidCredentialsException;
import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.auth.repository.InMemoryUserAccountRepository;
import com.example.urlshortener.auth.repository.UserAccountRepository;
import com.example.urlshortener.auth.token.AuthToken;
import com.example.urlshortener.auth.token.JwtTokenService;

class DefaultAuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private UserAccountRepository userAccountRepository;
    private JwtTokenService jwtTokenService;
    private DefaultAuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        userAccountRepository = new InMemoryUserAccountRepository();
        jwtTokenService = new StubJwtTokenService();
        authService = new DefaultAuthService(userAccountRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        authService.register(new UserRegistrationRequest(
                "  Samhita  ",
                "  SAMHITA@example.COM  ",
                "StrongPass123!"));

        UserAccount userAccount = userAccountRepository.findByEmail("samhita@example.com").orElseThrow();

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

    @Test
    void loginReturnsUserWhenPasswordMatchesStoredHash() {
        authService.register(new UserRegistrationRequest(
                "Samhita",
                "samhita@example.com",
                "StrongPass123!"));

        UserLoginResponse response = authService.login(new UserLoginRequest(
                "  SAMHITA@example.COM ",
                "StrongPass123!"));

        assertThat(response.name()).isEqualTo("Samhita");
        assertThat(response.email()).isEqualTo("samhita@example.com");
        assertThat(response.accessToken()).isEqualTo("test-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void loginRejectsUnknownEmailWithGenericMessage() {
        UserLoginRequest request = new UserLoginRequest(
                "missing@example.com",
                "StrongPass123!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void loginRejectsWrongPasswordWithGenericMessage() {
        authService.register(new UserRegistrationRequest(
                "Samhita",
                "samhita@example.com",
                "StrongPass123!"));

        UserLoginRequest request = new UserLoginRequest(
                "samhita@example.com",
                "WrongPass123!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    private static class StubJwtTokenService extends JwtTokenService {

        StubJwtTokenService() {
            super(new com.example.urlshortener.config.JwtProperties(
                    "test-issuer",
                    "test-secret-must-be-at-least-32-bytes",
                    java.time.Duration.ofMinutes(15)));
        }

        @Override
        public AuthToken createAccessToken(UserAccount userAccount) {
            return new AuthToken("test-jwt-token", "Bearer", 900);
        }
    }
}
