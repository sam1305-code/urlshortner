package com.example.urlshortener.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.of());
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String error,
            String message,
            String path,
            List<FieldValidationError> fieldErrors) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.copyOf(fieldErrors));
    }
}
