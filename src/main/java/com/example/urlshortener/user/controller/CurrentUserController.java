package com.example.urlshortener.user.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.user.dto.CurrentUserResponse;

@RestController
public class CurrentUserController {

    @GetMapping("/api/v1/users/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponse(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"));
    }
}
