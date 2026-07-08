# Distributed URL Shortener

A production-style URL shortener built with Java 21, Spring Boot, Couchbase, Redis, JWT authentication, Maven, Docker Compose, Swagger/OpenAPI, GitHub Actions, JUnit, and Mockito.

This project is designed as a backend engineering portfolio project: it shows API design, security, document database modeling, caching, validation, testing, and CI in a small but realistic system.

## Features

- User registration and login
- JWT-based authentication for protected APIs
- Create short URLs
- Custom aliases
- URL expiration
- Public redirect endpoint
- Click analytics
- QR code generation
- Search, pagination, and user dashboard-style listing
- Soft delete
- Rate limiting
- Couchbase-backed repositories
- Redis redirect cache with fail-open behavior
- Health checks
- Swagger/OpenAPI documentation
- GitHub Actions CI

## Tech Stack

- Java 21
- Spring Boot 4.1
- Spring Security OAuth2 Resource Server
- Couchbase Community Server
- Redis
- Maven
- Docker Compose
- Springdoc OpenAPI
- JUnit and Mockito
- GitHub Actions

## Architecture

The code follows a layered backend structure:

```text
Controller -> Service -> Repository -> Couchbase / Redis
     |            |             |
    DTOs       Domain        Document mapping
     |
Validation and exception handling
```

Important packages:

```text
com.example.urlshortener.auth        registration, login, users, JWT issuing
com.example.urlshortener.security    Spring Security and JWT validation
com.example.urlshortener.url         short URL creation, listing, redirect resolution
com.example.urlshortener.analytics   click event recording and analytics
com.example.urlshortener.qr          QR code generation
com.example.urlshortener.ratelimit   token-bucket rate limiting
com.example.urlshortener.config      application configuration
com.example.urlshortener.exception   global API error handling
```

## Couchbase Design

Couchbase is the primary database. It fits this project because the hottest access pattern is document-oriented: resolve one short code into one URL document as quickly as possible.

Main document keys:

```text
shortUrl::{shortCode}
user::{sha256(normalizedEmail)}
click::{shortCode}::{eventId}
```

The short URL key is deterministic, so redirect lookup does not need a secondary-index query. Dashboard and analytics queries use secondary indexes.

See [docs/couchbase.md](docs/couchbase.md) for schemas, indexes, and design notes.

## Redis Caching

Redis is used for redirect caching. Couchbase remains the source of truth.

```text
short-url:{shortCode}
```

The cache is fail-open: if Redis read, write, serialization, or delete fails, the application logs the issue and continues using the repository path. This protects the redirect flow from cache outages.

See [docs/redis-caching.md](docs/redis-caching.md) for cache behavior and TTL design.

## Running Locally

Prerequisites:

- Java 21
- Maven
- Docker Desktop

Start infrastructure:

```bash
docker compose up -d
```

Open Couchbase at:

```text
http://localhost:8091
```

Create a single-node cluster with:

```text
Username: Administrator
Password: password
Bucket: url_shortener
```

Create the Couchbase indexes from [docs/couchbase.md](docs/couchbase.md).

Run the app with Couchbase and Redis:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=couchbase,redis
```

Run without external infrastructure, using in-memory repositories:

```bash
mvn spring-boot:run
```

## API Documentation

After starting the app:

```text
Swagger UI:      http://localhost:8080/swagger-ui.html
OpenAPI JSON:   http://localhost:8080/v3/api-docs
Health check:   http://localhost:8080/actuator/health
System ping:    http://localhost:8080/api/v1/system/ping
```

## Main API Endpoints

Public:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /{shortCode}
GET  /api/v1/system/ping
GET  /actuator/health
```

Authenticated:

```text
GET    /api/v1/users/me
POST   /api/v1/urls
GET    /api/v1/urls?search=&page=0&size=20
GET    /api/v1/urls/{shortCode}/analytics
GET    /api/v1/urls/{shortCode}/qr-code
DELETE /api/v1/urls/{shortCode}
```

## Example Flow

Register:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Samhita","email":"samhita@example.com","password":"Password123!"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"samhita@example.com","password":"Password123!"}'
```

Create a short URL:

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{"originalUrl":"https://example.com/docs","customAlias":"docs2026"}'
```

Redirect:

```bash
curl -i http://localhost:8080/docs2026
```

## Testing

Run the full test suite:

```bash
mvn test
```

Current test coverage includes:

- Auth service and controller behavior
- JWT creation and validation
- URL creation, custom aliases, expiration, listing, search, and soft delete
- Redirect controller behavior
- QR code generation
- Rate limiting
- Couchbase repository mapping logic
- Redis cache TTL and failure handling
- Security integration tests
- OpenAPI public documentation access

## CI

GitHub Actions runs:

```bash
mvn --batch-mode test
```

on pushes and pull requests to `main` and `master`.

## Production Notes

Before deploying:

- Replace the development JWT secret with a secure environment-specific secret.
- Run Couchbase as a managed or properly clustered service.
- Use Redis with persistence, monitoring, and eviction policy configured.
- Put the app behind HTTPS.
- Add structured logging and metrics dashboards.
- Restrict Swagger/OpenAPI in production if the API should not be publicly discoverable.
- Add admin endpoints with role-based access control.
- Add Azure deployment workflow.

## Roadmap

- Admin APIs for user and URL moderation
- Role-based authorization
- Dockerfile for the Spring Boot application
- Azure App Service or container deployment
- Code coverage reporting
- Integration tests with Testcontainers
- Redis-backed distributed rate limiting
- Analytics aggregation endpoints for time-series dashboards
