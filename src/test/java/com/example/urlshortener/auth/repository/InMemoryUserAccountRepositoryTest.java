package com.example.urlshortener.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.auth.model.UserAccount;

class InMemoryUserAccountRepositoryTest {

    private InMemoryUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserAccountRepository();
    }

    @Test
    void insertIfEmailAbsentStoresUserOnce() {
        UserAccount userAccount = userAccount("samhita@example.com");

        boolean firstInsert = repository.insertIfEmailAbsent(userAccount);
        boolean secondInsert = repository.insertIfEmailAbsent(userAccount);

        assertThat(firstInsert).isTrue();
        assertThat(secondInsert).isFalse();
    }

    @Test
    void findByEmailNormalizesLookupValue() {
        UserAccount userAccount = userAccount("samhita@example.com");
        repository.insertIfEmailAbsent(userAccount);

        assertThat(repository.findByEmail("  SAMHITA@example.COM "))
                .contains(userAccount);
    }

    private UserAccount userAccount(String email) {
        return new UserAccount(
                UUID.randomUUID(),
                "Samhita",
                email,
                "$2a$12$examplePasswordHash",
                Instant.parse("2026-07-08T10:15:30Z"));
    }
}
