# Redis Redirect Caching

Redis is used as a read-through cache for public redirects. Couchbase remains the source of truth.

## Local Setup

Start Redis with Docker Compose:

```bash
docker compose up -d redis
```

Run the application with Redis caching enabled:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

Run Couchbase and Redis together:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=couchbase,redis
```

## Cache Key

```text
short-url:{shortCode}
```

The cached value contains only data needed for redirect resolution:

```json
{
  "shortCode": "abc123XY",
  "originalUrl": "https://example.com/docs",
  "expiresAt": "2026-07-09T10:15:30Z"
}
```

## Why This Design

Redirects are the hottest read path in a URL shortener. Redis reduces repeated Couchbase reads for popular links while staying disposable: if Redis is empty, the app reads from Couchbase and repopulates the cache.

The cache TTL is capped by URL expiration. This prevents Redis from serving an expired URL after Couchbase would reject it.

Soft deletes evict the redirect cache entry so deleted links stop resolving.
