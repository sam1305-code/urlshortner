package com.example.urlshortener.auth.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.auth.util.EmailNormalizer;
import com.example.urlshortener.config.CouchbaseAppProperties;

@Repository
@Profile("couchbase")
public class CouchbaseUserAccountRepository implements UserAccountRepository {

    private static final String DOCUMENT_TYPE = "user_account";
    private static final String DOCUMENT_KEY_PREFIX = "user::";

    private final Collection collection;

    public CouchbaseUserAccountRepository(Cluster cluster, CouchbaseAppProperties couchbaseAppProperties) {
        this.collection = cluster.bucket(couchbaseAppProperties.bucketName()).defaultCollection();
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        try {
            return Optional.of(toDomain(collection.get(documentKey(email)).contentAsObject()));
        } catch (DocumentNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean insertIfEmailAbsent(UserAccount userAccount) {
        try {
            collection.insert(documentKey(userAccount.email()), toDocument(userAccount));
            return true;
        } catch (DocumentExistsException exception) {
            return false;
        }
    }

    private JsonObject toDocument(UserAccount userAccount) {
        return JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("id", userAccount.id().toString())
                .put("name", userAccount.name())
                .put("email", userAccount.email())
                .put("passwordHash", userAccount.passwordHash())
                .put("createdAt", userAccount.createdAt().toString());
    }

    private UserAccount toDomain(JsonObject document) {
        return new UserAccount(
                UUID.fromString(document.getString("id")),
                document.getString("name"),
                document.getString("email"),
                document.getString("passwordHash"),
                Instant.parse(document.getString("createdAt")));
    }

    private String documentKey(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return DOCUMENT_KEY_PREFIX + sha256Hex(normalizedEmail);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for user document keys", exception);
        }
    }
}
