package com.example.urlshortener.analytics.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.config.CouchbaseAppProperties;

@Repository
@Profile("couchbase")
public class CouchbaseClickEventRepository implements ClickEventRepository {

    private static final String DOCUMENT_TYPE = "click_event";
    private static final String DOCUMENT_KEY_PREFIX = "click::";

    private final Cluster cluster;
    private final Collection collection;
    private final String bucketName;

    public CouchbaseClickEventRepository(Cluster cluster, CouchbaseAppProperties couchbaseAppProperties) {
        this.cluster = cluster;
        this.bucketName = couchbaseAppProperties.bucketName();
        this.collection = cluster.bucket(bucketName).defaultCollection();
    }

    @Override
    public void save(ClickEvent clickEvent) {
        collection.insert(documentKey(clickEvent), toDocument(clickEvent));
    }

    @Override
    public long countByShortCode(String shortCode) {
        String statement = """
                SELECT COUNT(1) AS totalClicks
                FROM `%s`
                WHERE type = $type AND shortCode = $shortCode
                """.formatted(escapedBucketName());

        JsonObject parameters = JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("shortCode", shortCode);

        return cluster.query(statement, QueryOptions.queryOptions().parameters(parameters))
                .rowsAsObject()
                .getFirst()
                .getLong("totalClicks");
    }

    @Override
    public List<ClickEvent> findByShortCode(String shortCode) {
        String statement = """
                SELECT id, shortCode, clickedAt, ipAddress, userAgent, referer
                FROM `%s`
                WHERE type = $type AND shortCode = $shortCode
                ORDER BY clickedAt DESC
                """.formatted(escapedBucketName());

        JsonObject parameters = JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("shortCode", shortCode);

        return cluster.query(statement, QueryOptions.queryOptions().parameters(parameters))
                .rowsAsObject()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private JsonObject toDocument(ClickEvent clickEvent) {
        JsonObject document = JsonObject.create()
                .put("type", DOCUMENT_TYPE)
                .put("id", clickEvent.id().toString())
                .put("shortCode", clickEvent.shortCode())
                .put("clickedAt", clickEvent.clickedAt().toString())
                .put("ipAddress", clickEvent.ipAddress())
                .put("userAgent", clickEvent.userAgent());

        if (clickEvent.referer() != null) {
            document.put("referer", clickEvent.referer());
        }

        return document;
    }

    private ClickEvent toDomain(JsonObject document) {
        return new ClickEvent(
                UUID.fromString(document.getString("id")),
                document.getString("shortCode"),
                Instant.parse(document.getString("clickedAt")),
                document.getString("ipAddress"),
                document.getString("userAgent"),
                document.getString("referer"));
    }

    private String documentKey(ClickEvent clickEvent) {
        return DOCUMENT_KEY_PREFIX + clickEvent.shortCode() + "::" + clickEvent.id();
    }

    private String escapedBucketName() {
        return bucketName.replace("`", "``");
    }
}
