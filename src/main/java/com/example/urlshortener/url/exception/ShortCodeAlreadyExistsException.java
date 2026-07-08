package com.example.urlshortener.url.exception;

public class ShortCodeAlreadyExistsException extends RuntimeException {

    public ShortCodeAlreadyExistsException(String shortCode) {
        super("Short code is already in use: " + shortCode);
    }
}
