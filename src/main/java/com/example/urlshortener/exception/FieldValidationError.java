package com.example.urlshortener.exception;

public record FieldValidationError(String field, String message) {
}
