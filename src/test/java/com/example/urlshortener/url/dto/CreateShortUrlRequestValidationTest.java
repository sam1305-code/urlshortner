package com.example.urlshortener.url.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateShortUrlRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validHttpsUrlAndCustomAliasPassValidation() {
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://example.com/docs",
                "my-alias_123",
                null);

        Set<ConstraintViolation<CreateShortUrlRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void customAliasMustMatchAllowedShape() {
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://example.com/docs",
                "-bad",
                null);

        Set<ConstraintViolation<CreateShortUrlRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("customAlias");
    }

    @Test
    void originalUrlMustUseHttpOrHttps() {
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "ftp://example.com/docs",
                null,
                null);

        Set<ConstraintViolation<CreateShortUrlRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("originalUrl");
    }
}
