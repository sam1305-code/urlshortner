package com.example.urlshortener.url.exception;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String shortCode) {
        super("Short URL was not found: " + shortCode);
    }
}
