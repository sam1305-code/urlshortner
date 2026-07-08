package com.example.urlshortener.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.json.JsonObject;
import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.config.CouchbaseAppProperties;

class CouchbaseUserAccountRepositoryTest {

    private static final String EXPECTED_DOCUMENT_KEY =
            "user::34336f8cdabdf599c0e33d2dc5bdf1f1b841c6536f990388af95625aecf80f04";

    private Collection collection;
    private CouchbaseUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        Cluster cluster = mock(Cluster.class);
        Bucket bucket = mock(Bucket.class);
        collection = mock(Collection.class);

        when(cluster.bucket("url_shortener")).thenReturn(bucket);
        when(bucket.defaultCollection()).thenReturn(collection);

        repository = new CouchbaseUserAccountRepository(cluster, new CouchbaseAppProperties("url_shortener"));
    }

    @Test
    void insertIfEmailAbsentUsesHashedNormalizedEmailKey() {
        UserAccount userAccount = userAccount("samhita@example.com");

        boolean inserted = repository.insertIfEmailAbsent(userAccount);

        assertThat(inserted).isTrue();
        verify(collection).insert(eq(EXPECTED_DOCUMENT_KEY), any(JsonObject.class));
    }

    @Test
    void insertIfEmailAbsentReturnsFalseWhenDocumentAlreadyExists() {
        UserAccount userAccount = userAccount("samhita@example.com");
        when(collection.insert(eq(EXPECTED_DOCUMENT_KEY), any(JsonObject.class)))
                .thenThrow(new DocumentExistsException(null));

        boolean inserted = repository.insertIfEmailAbsent(userAccount);

        assertThat(inserted).isFalse();
    }

    @Test
    void findByEmailNormalizesLookupAndMapsDocument() {
        GetResult getResult = mock(GetResult.class);
        when(collection.get(EXPECTED_DOCUMENT_KEY)).thenReturn(getResult);
        when(getResult.contentAsObject()).thenReturn(userDocument());

        Optional<UserAccount> userAccount = repository.findByEmail("  SAMHITA@example.COM ");

        assertThat(userAccount)
                .contains(userAccount("samhita@example.com"));
    }

    @Test
    void findByEmailReturnsEmptyWhenDocumentDoesNotExist() {
        when(collection.get(EXPECTED_DOCUMENT_KEY)).thenThrow(new DocumentNotFoundException(null));

        Optional<UserAccount> userAccount = repository.findByEmail("samhita@example.com");

        assertThat(userAccount).isEmpty();
    }

    private UserAccount userAccount(String email) {
        return new UserAccount(
                UUID.fromString("9abda1f4-6dd0-4c92-9326-91f0846f2e4f"),
                "Samhita",
                email,
                "$2a$12$examplePasswordHash",
                Instant.parse("2026-07-08T10:15:30Z"));
    }

    private JsonObject userDocument() {
        return JsonObject.create()
                .put("type", "user_account")
                .put("id", "9abda1f4-6dd0-4c92-9326-91f0846f2e4f")
                .put("name", "Samhita")
                .put("email", "samhita@example.com")
                .put("passwordHash", "$2a$12$examplePasswordHash")
                .put("createdAt", "2026-07-08T10:15:30Z");
    }
}
