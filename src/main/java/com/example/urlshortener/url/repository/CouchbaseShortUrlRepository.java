package com.example.urlshortener.url.repository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.example.urlshortener.config.CouchbaseAppProperties;
import com.example.urlshortener.url.model.ShortUrl;

@Repository
@Profile("couchbase")
public class CouchbaseShortUrlRepository implements ShortUrlRepository {

    private static final String DOCUMENT_TYPE = "short_url";
    private static final String DOCUMENT_KEY_PREFIX = "shortUrl::";

    private final Cluster cluster;
    private final Collection collection;
    private final String bucketName;

    public CouchbaseShortUrlRepository(Cluster cluster, CouchbaseAppProperties couchbaseAppProperties) {
        this.cluster = cluster;
        this.bucketName = couchbaseAppProperties.bucketName();
        this.collection = cluster.bucket(bucketName).defaultCollection();
    }

    @Override
    public boolean insertIfShortCodeAbsent(ShortUrl shortUrl) {
        try {
            collection.insert(documentKey(shortUrl.shortCode()), toDocument(shortUrl));
            return true;
        } catch (DocumentExistsException exception) {
            return false;
        }
    }

    @Override
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        try {
            return Optional.of(toDomain(collection.get(documentKey(shortCode)).contentAsObject()));
        } catch (com.couchbase.client.core.error.DocumentNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ShortUrl> findByShortCodeAndOwnerId(String shortCode, UUID ownerId) {
        return findByShortCode(shortCode)
                .filter(shortUrl -> shortUrl.ownerId().equals(ownerId));
    }

    @Override
    public List<ShortUrl> findActiveByOwnerId(UUID ownerId, String searchTerm) {
        JsonObject parameters = JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("ownerId", ownerId.toString());
        String searchClause = "";
        if (searchTerm != null && !searchTerm.isBlank()) {
            searchClause = " AND (LOWER(shortCode) LIKE $search OR LOWER(originalUrl) LIKE $search)";
            parameters.put("search", "%" + searchTerm.trim().toLowerCase(Locale.ROOT) + "%");
        }

        String statement = """
                SELECT id, shortCode, originalUrl, ownerId, createdAt, expiresAt, deleted
                FROM `%s`
                WHERE type = $type AND ownerId = $ownerId AND deleted = false%s
                ORDER BY createdAt DESC
                """.formatted(escapedBucketName(), searchClause);

        return cluster.query(statement, QueryOptions.queryOptions().parameters(parameters))
                .rowsAsObject()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        collection.upsert(documentKey(shortUrl.shortCode()), toDocument(shortUrl));
        return shortUrl;
    }

    private JsonObject toDocument(ShortUrl shortUrl) {
        JsonObject document = JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("id", shortUrl.id().toString())
                .put("shortCode", shortUrl.shortCode())
                .put("originalUrl", shortUrl.originalUrl())
                .put("ownerId", shortUrl.ownerId().toString())
                .put("createdAt", shortUrl.createdAt().toString())
                .put("deleted", shortUrl.deleted());

        if (shortUrl.expiresAt() != null) {
            document.put("expiresAt", shortUrl.expiresAt().toString());
        }

        return document;
    }

    private ShortUrl toDomain(JsonObject document) {
        return new ShortUrl(
                UUID.fromString(document.getString("id")),
                document.getString("shortCode"),
                document.getString("originalUrl"),
                UUID.fromString(document.getString("ownerId")),
                Instant.parse(document.getString("createdAt")),
                document.containsKey("expiresAt") ? Instant.parse(document.getString("expiresAt")) : null,
                document.getBoolean("deleted"));
    }

    private String documentKey(String shortCode) {
        return DOCUMENT_KEY_PREFIX + shortCode;
    }

    private String escapedBucketName() {
        return bucketName.replace("`", "``");
    }
}
