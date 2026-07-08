package com.example.urlshortener.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.auth.token.JwtTokenService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenService jwtTokenService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void protectedEndpointRejectsMissingBearerToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/users/me"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        UUID userId = UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f");
        UserAccount userAccount = new UserAccount(
                userId,
                "Samhita",
                "samhita@example.com",
                "$2a$12$passwordHash",
                Instant.parse("2026-07-08T10:00:00Z"));
        String accessToken = jwtTokenService.createAccessToken(userAccount).accessToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/users/me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(userId.toString());
        assertThat(response.body()).contains("Samhita");
        assertThat(response.body()).contains("samhita@example.com");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
