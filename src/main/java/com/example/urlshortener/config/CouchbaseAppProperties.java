package com.example.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.couchbase")
public record CouchbaseAppProperties(
        String bucketName) {
}
