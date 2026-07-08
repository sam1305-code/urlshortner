package com.example.urlshortener.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    @GetMapping("/api/v1/system/ping")
    public SystemStatus ping() {
        return new SystemStatus("ok", Instant.now());
    }

    public record SystemStatus(String status, Instant timestamp) {
    }
}
