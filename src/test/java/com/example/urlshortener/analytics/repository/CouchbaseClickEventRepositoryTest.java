package com.example.urlshortener.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.example.urlshortener.analytics.model.ClickEvent;
import com.example.urlshortener.config.CouchbaseAppProperties;

class CouchbaseClickEventRepositoryTest {

    private static final UUID CLICK_ID = UUID.fromString("48d44fb6-bb15-4882-8786-12f536439cb0");
    private static final String EXPECTED_DOCUMENT_KEY = "click::abc123XY::48d44fb6-bb15-4882-8786-12f536439cb0";

    private Cluster cluster;
    private Collection collection;
    private CouchbaseClickEventRepository repository;

    @BeforeEach
    void setUp() {
        cluster = mock(Cluster.class);
        Bucket bucket = mock(Bucket.class);
        collection = mock(Collection.class);

        when(cluster.bucket("url_shortener")).thenReturn(bucket);
        when(bucket.defaultCollection()).thenReturn(collection);

        repository = new CouchbaseClickEventRepository(cluster, new CouchbaseAppProperties("url_shortener"));
    }

    @Test
    void saveStoresClickEventDocumentWithDeterministicKey() {
        ClickEvent clickEvent = clickEvent("https://referrer.example.com");
        ArgumentCaptor<JsonObject> documentCaptor = ArgumentCaptor.forClass(JsonObject.class);

        repository.save(clickEvent);

        verify(collection).insert(eq(EXPECTED_DOCUMENT_KEY), documentCaptor.capture());
        JsonObject document = documentCaptor.getValue();
        assertThat(document.getString("type")).isEqualTo("click_event");
        assertThat(document.getString("id")).isEqualTo(CLICK_ID.toString());
        assertThat(document.getString("shortCode")).isEqualTo("abc123XY");
        assertThat(document.getString("clickedAt")).isEqualTo("2026-07-08T10:15:30Z");
        assertThat(document.getString("ipAddress")).isEqualTo("203.0.113.10");
        assertThat(document.getString("userAgent")).isEqualTo("Mozilla/5.0");
        assertThat(document.getString("referer")).isEqualTo("https://referrer.example.com");
    }

    @Test
    void saveOmitsRefererWhenMissing() {
        ArgumentCaptor<JsonObject> documentCaptor = ArgumentCaptor.forClass(JsonObject.class);

        repository.save(clickEvent(null));

        verify(collection).insert(eq(EXPECTED_DOCUMENT_KEY), documentCaptor.capture());
        assertThat(documentCaptor.getValue().containsKey("referer")).isFalse();
    }

    @Test
    void countByShortCodeReturnsCountFromQueryResult() {
        QueryResult queryResult = mock(QueryResult.class);
        when(cluster.query(contains("COUNT(1)"), any(QueryOptions.class))).thenReturn(queryResult);
        when(queryResult.rowsAsObject()).thenReturn(List.of(JsonObject.create().put("totalClicks", 42L)));

        long totalClicks = repository.countByShortCode("abc123XY");

        assertThat(totalClicks).isEqualTo(42);
    }

    @Test
    void findByShortCodeMapsRowsToDomainEvents() {
        QueryResult queryResult = mock(QueryResult.class);
        when(cluster.query(contains("ORDER BY clickedAt DESC"), any(QueryOptions.class))).thenReturn(queryResult);
        when(queryResult.rowsAsObject()).thenReturn(List.of(clickEventDocument()));

        List<ClickEvent> clickEvents = repository.findByShortCode("abc123XY");

        assertThat(clickEvents).containsExactly(clickEvent("https://referrer.example.com"));
    }

    private ClickEvent clickEvent(String referer) {
        return new ClickEvent(
                CLICK_ID,
                "abc123XY",
                Instant.parse("2026-07-08T10:15:30Z"),
                "203.0.113.10",
                "Mozilla/5.0",
                referer);
    }

    private JsonObject clickEventDocument() {
        return JsonObject.create()
                .put("id", CLICK_ID.toString())
                .put("shortCode", "abc123XY")
                .put("clickedAt", "2026-07-08T10:15:30Z")
                .put("ipAddress", "203.0.113.10")
                .put("userAgent", "Mozilla/5.0")
                .put("referer", "https://referrer.example.com");
    }
}
