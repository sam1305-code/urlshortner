package com.example.urlshortener.auth.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.urlshortener.auth.dto.UserLoginRequest;
import com.example.urlshortener.auth.dto.UserLoginResponse;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.exception.EmailAlreadyRegisteredException;
import com.example.urlshortener.auth.exception.InvalidCredentialsException;
import com.example.urlshortener.auth.service.AuthService;
import com.example.urlshortener.exception.GlobalExceptionHandler;

class AuthControllerTest {

    private StubAuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = new StubAuthService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreatedUserWithoutPassword() throws Exception {
        UUID userId = UUID.fromString("f97fc7f7-7a44-4a5e-906a-c28fda59cb33");
        authService.registerHandler = request -> new UserRegistrationResponse(
                userId,
                "Samhita",
                "samhita@example.com",
                Instant.parse("2026-07-08T10:15:30Z"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Samhita",
                                  "email": "samhita@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/users/" + userId))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Samhita"))
                .andExpect(jsonPath("$.email").value("samhita@example.com"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        authService.registerHandler = request -> {
            throw new EmailAlreadyRegisteredException("samhita@example.com");
        };

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Samhita",
                                  "email": "samhita@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email is already registered."));
    }

    @Test
    void loginReturnsAuthenticatedUserWithoutPassword() throws Exception {
        UUID userId = UUID.fromString("2a6d43b0-efc6-4ec5-b1e3-448bb7dd7462");
        authService.loginHandler = request -> new UserLoginResponse(
                userId,
                "Samhita",
                "samhita@example.com",
                "jwt-token",
                "Bearer",
                900);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "samhita@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Samhita"))
                .andExpect(jsonPath("$.email").value("samhita@example.com"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        authService.loginHandler = request -> {
            throw new InvalidCredentialsException();
        };

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "samhita@example.com",
                                  "password": "WrongPass123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    private static class StubAuthService implements AuthService {

        private Function<com.example.urlshortener.auth.dto.UserRegistrationRequest, UserRegistrationResponse> registerHandler =
                request -> new UserRegistrationResponse(
                        UUID.randomUUID(),
                        request.name(),
                        request.email(),
                        Instant.now());
        private Function<UserLoginRequest, UserLoginResponse> loginHandler =
                request -> new UserLoginResponse(
                        UUID.randomUUID(),
                        "Samhita",
                        request.email(),
                        "jwt-token",
                        "Bearer",
                        900);

        @Override
        public UserRegistrationResponse register(com.example.urlshortener.auth.dto.UserRegistrationRequest request) {
            return registerHandler.apply(request);
        }

        @Override
        public UserLoginResponse login(UserLoginRequest request) {
            return loginHandler.apply(request);
        }
    }
}
