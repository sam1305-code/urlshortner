package com.example.urlshortener.auth.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;
import com.example.urlshortener.auth.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserRegistrationResponse response = authService.register(request);
        URI location = URI.create("/api/v1/users/" + response.id());

        return ResponseEntity.created(location).body(response);
    }
}
