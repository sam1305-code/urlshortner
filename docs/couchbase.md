# Couchbase Design

## Local Setup

Start Couchbase:

```bash
docker compose up -d couchbase
```

Open `http://localhost:8091`, create a single-node cluster, and use:

```text
Username: Administrator
Password: password
Bucket: url_shortener
```

Run the app with the Couchbase profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=couchbase
```

## Short URL Document

Document key:

```text
shortUrl::{shortCode}
```

Example document:

```json
{
  "type": "short_url",
  "id": "48d44fb6-bb15-4882-8786-12f536439cb0",
  "shortCode": "abc123XY",
  "originalUrl": "https://example.com/docs",
  "ownerId": "9abda1f4-6dd0-4c92-9326-91f0846f2e4f",
  "createdAt": "2026-07-08T10:15:30Z",
  "expiresAt": "2026-07-09T10:15:30Z",
  "deleted": false
}
```

## Indexes

Create these from the Couchbase Query Workbench:

```sql
CREATE PRIMARY INDEX IF NOT EXISTS ON `url_shortener`;

CREATE INDEX IF NOT EXISTS idx_short_url_owner_dashboard
ON `url_shortener`(ownerId, deleted, createdAt DESC)
WHERE type = "short_url";

CREATE INDEX IF NOT EXISTS idx_short_url_owner_search
ON `url_shortener`(ownerId, deleted, LOWER(shortCode), LOWER(originalUrl))
WHERE type = "short_url";
```

## Why Couchbase Fits

Couchbase is a good fit for a URL shortener because the main access pattern is document-oriented:

- Read one short URL by key during redirects.
- Write one short URL document during creation.
- Query a user-owned subset for the dashboard.
- Keep flexible analytics documents without heavy relational joins.

The short-code lookup uses a deterministic document key, which is faster and simpler than querying by alias.

## User Account Document

Document key:

```text
user::{sha256(normalizedEmail)}
```

Example document:

```json
{
  "type": "user_account",
  "id": "9abda1f4-6dd0-4c92-9326-91f0846f2e4f",
  "name": "Samhita",
  "email": "samhita@example.com",
  "passwordHash": "$2a$12$...",
  "createdAt": "2026-07-08T10:15:30Z"
}
```

The email is normalized before hashing, so `SAMHITA@example.com` and `samhita@example.com` map to the same document. The key is hashed because document IDs often appear in logs, metrics, and support tooling; hashing avoids placing raw email addresses in those locations.

Additional user index:

```sql
CREATE INDEX IF NOT EXISTS idx_user_account_created_at
ON `url_shortener`(createdAt DESC)
WHERE type = "user_account";
```

## Click Event Document

Document key:

```text
click::{shortCode}::{eventId}
```

Example document:

```json
{
  "type": "click_event",
  "id": "48d44fb6-bb15-4882-8786-12f536439cb0",
  "shortCode": "abc123XY",
  "clickedAt": "2026-07-08T10:15:30Z",
  "ipAddress": "203.0.113.10",
  "userAgent": "Mozilla/5.0",
  "referer": "https://referrer.example.com"
}
```

Click events are append-only. This keeps redirects simple: resolve the short URL, write one event, and return the redirect response. Later, analytics queries can aggregate events by `shortCode`.

Click analytics index:

```sql
CREATE INDEX IF NOT EXISTS idx_click_event_short_code_clicked_at
ON `url_shortener`(shortCode, clickedAt DESC)
WHERE type = "click_event";
```
