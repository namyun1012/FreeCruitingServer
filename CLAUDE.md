# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**FreeCruiting** is a team-recruiting platform (Spring Boot 3.4.1, Java 17) where users can post recruiting notices, form parties (teams), chat in real time, and receive live notifications. The server runs HTTPS on port **8443**.

---

## Commands

### Build & Run
```bash
./gradlew bootRun           # Start the application (HTTPS localhost:8443)
./gradlew build             # Full build → build/libs/*.jar
./gradlew clean build -x test  # Build skipping tests
```

### Tests
```bash
./gradlew test                                                      # All tests
./gradlew test --tests com.project.freecruting.FreecrutingApplicationTests
./gradlew test --tests com.project.freecruting.controller.PostApiControllerTest
./gradlew test --tests com.project.freecruting.model.PostRepositoryTest
```

### Docker (full stack)
```bash
docker-compose up   # App + PostgreSQL + Redis + Prometheus + Grafana
```

---

## Prerequisites

| Service    | Default                         | Config key                         |
|------------|---------------------------------|------------------------------------|
| PostgreSQL | `localhost:5432/FreeCruiting`   | `.env`: `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| Redis      | `localhost:6379`                | `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` |
| HTTPS cert | `src/main/resources/keystore.p12` (PKCS12, alias `springboot`) | `application.yml` |

Schema is managed by Hibernate `ddl-auto: update` — no migration tool.

---

## Architecture

### Package layout (`src/main/java/com/project/freecruting/`)

```
controller/       REST endpoints (base path /api/v1)
service/          Business logic
  infra/storage/  FileService interface → LocalFileService | S3FileService
repository/       Spring Data JPA repositories
model/            JPA entities + type enums
dto/              Request/Response DTOs per domain (post/, party/, user/, comment/, notification/)
config/auth/      Spring Security + OAuth2 + LoginUser annotation
handler/          GlobalExceptionHandler (@ControllerAdvice)
exception/        NotFoundException, ForbiddenException, InvalidStateException
events/           NotificationEventListener
log/              Logging utilities
```

### Key architectural decisions

**`@LoginUser` annotation** — A custom `@Target(PARAMETER)` annotation resolved by `LoginUserArgumentResolver`. Inject `SessionUser` (a lightweight session DTO, not the JPA entity) into any controller method to get the authenticated user without hitting the DB.

**DTO pattern** — DTOs live in `dto/<domain>/`. Request DTOs include a `toEntity()` method. Response DTOs are constructed from entities in the service layer.

**Redis view counting** — Post view counts are incremented in Redis (24-hour dedup per user) and flushed to PostgreSQL every 5 minutes via `@Scheduled`. If Redis is unavailable, the service falls back to a direct DB increment.

**Real-time stack**:
- *Chat*: WebSocket + STOMP over SockJS at `/ws-stomp`. Publish prefix `/pub`, subscribe prefix `/sub`.
- *Notifications*: `SseEmitterService` manages per-user `SseEmitter` connections. `NotificationEventListener` persists a `Notification` entity and pushes it over SSE.

**File storage** — Injected as `FileService`. Switch between `LocalFileService` (default, `./uploads`) and `S3FileService` (AWS) via `application.yml`.

**Security** — Session-based (Redis-backed via Spring Session) + Google OAuth2. The `@LoginUser` parameter returns `null` for unauthenticated requests; guard accordingly. CSRF is currently disabled.

### Domains & entities

| Entity            | Purpose                                      |
|-------------------|----------------------------------------------|
| `User`            | Accounts; supports OAuth2 + form login; has `Role` |
| `Post`            | Recruiting posts; types: PROJECT, STUDY, REVIEW, ANNOUNCEMENT |
| `Comment`         | Comments on posts                            |
| `Party`           | Teams (5–15 members)                         |
| `PartyMember`     | Party membership join table                  |
| `PartyJoinRequest`| Join requests with status enum               |
| `Notification`    | Persisted notifications; delivered via SSE   |
| `ChatMessage`     | WebSocket chat messages                      |

All entities extend `BaseTimeEntity` which provides `createdDate` and `modifiedDate` via `@EnableJpaAuditing`.

---

## Configuration files

- `src/main/resources/application.yml` — main config (server, DB, Redis, async thread pool, Prometheus)
- `src/main/resources/application-oauth.properties` — Google OAuth2 credentials
- `.env` — `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`
