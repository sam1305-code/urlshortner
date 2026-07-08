package com.example.urlshortener.url.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class SecureRandomShortCodeGenerator implements ShortCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int DEFAULT_CODE_LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        char[] code = new char[DEFAULT_CODE_LENGTH];
        for (int index = 0; index < code.length; index++) {
            code[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
