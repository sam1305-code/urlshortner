package com.example.urlshortener.url.exception;

public class InvalidPageRequestException extends RuntimeException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
