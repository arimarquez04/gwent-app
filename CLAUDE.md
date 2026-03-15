# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

Each service is built and run independently with Maven. From the service directory:

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MyTestClass

# Run a single test method
./mvnw test -Dtest=MyTestClass#myMethod
```

On Windows without the Maven wrapper:
```bash
mvn spring-boot:run
```

Services must be started independently; there is no root aggregator POM.

## Architecture

Spring Boot 3.x microservices (Java 21) with a BFF pattern. All public traffic enters through `bff-service` on port 8080. Internal services communicate over plain HTTP REST using Spring's `RestClient`.

**Service ports:**
- `bff-service` — 8080 (public entry, `/api/**`)
- `jugador-service` — 8082
- `ingame-service` — 8083
- `auth-service` — 8085 (`/auth/v1`)
- `solicitud-service` — port not yet assigned

**Database:** MySQL on `localhost:3306`, database `gwent`, user `root`/`admin`. All services share the same DB instance.

## gwent-commons Library

All services depend on a shared library (`gwent-commons`, version `0.11.0`) published to local Maven. Key packages:

| Module | Key types |
|---|---|
| `gwent-common` | `com.arimar.gwent.common.response.GenericResponseDTO` (used in internal calls) / `com.arimar.gwent.common.utils.response.GenericResponseDTO` (used in BFF controllers — note the different package path) |
| `gwent-contracts` | `com.arimar.gwent.contracts.auth.request.{LoginRequest, RegisterRequest}`, `com.arimar.gwent.contracts.auth.response.TokenResponse`, `com.arimar.gwent.contracts.auth.claims.JwtClaimNames` |
| `gwent-security` | `com.arimar.gwent.security.actor.Actor` (fields: userId, gameId, username, tag) |
| `gwent-communication` | `com.arimar.gwent.communication.invoker.{GenericRestInvoker, ServiceCallResponse}`, `com.arimar.gwent.communication.error.RemoteServiceErrorMapper` |
| `gwent-domain` | `com.arimar.gwent.domain.user.UserEntity` |

## Identity & Security Rules

- JWT is RS256, signed by `auth-service` with keys in `resources/keys/private.pem` / `public.pem`.
- All services validate JWTs with the same `public.pem` via Spring OAuth2 resource server.
- JWT claims: `sub` = userId, `username`, `gameId`, `tag`.
- **The actor's `playerId` is never sent by the client in the request body.** It is always extracted from the JWT via `ActorResolver` on the server side.
- BFF propagates the Bearer token as-is when calling internal services.

## Internal Communication Pattern

Internal service calls use `GenericRestInvoker` from `gwent-communication`:

```java
ServiceCallResponse<T> response = invoker.exchange(method, url, body, headers, okType);
if (response.isOk()) return response.ok();
throw errorMapper.toException(serviceName, url, response.httpStatus(), response.error());
```

Service URLs are configured in `application.yml` under `gwent.services.*` and injected via `@ConfigurationProperties` classes (e.g., `JugadorServiceConfig`, `AuthServiceConfig`).

## Error Handling Pattern

Every service must have a `GlobalExceptionHandler` (`@RestControllerAdvice`) that always returns `ErrorDTO` from `com.arimar.gwent.common.exception.ErrorDTO`. This is required for `GenericRestInvoker` (in gwent-communication) to correctly parse error responses from upstream services.

**Per layer:**
- **Internal services** (jugador-service, auth-service): handle domain exceptions → return `ErrorDTO` with the real HTTP status.
- **BFF**: catches `InternalService4xxErrorException` / `InternalService5xxErrorException` (thrown by `RemoteServiceErrorMapper`) → propagates the upstream `ErrorDTO` to the client. Also handles `BadRequestException` for validation errors.

Exception handlers location:
- `auth-service`: `com.arimar.gwent.authservice.exception.ApiExceptionHandler`
- `jugador-service`: `com.arimar.gwent.jugadorservice.exception.GlobalExceptionHandler`
- `bff-service`: `com.arimar.gwent.bff.exception.GlobalExceptionHandler`

## Adding New Functionality — Checklist

### If the feature lives entirely in one internal service (e.g., jugador-service)

1. **Entity / DB** — add or modify `@Entity` fields, let `ddl-auto: update` apply the schema change.
2. **Repository** — add query methods to the `JpaRepository` if needed.
3. **Service** — implement business logic; throw `ResponseStatusException` for domain errors (404, 409, etc.).
4. **DTOs** — create request/response DTOs in the `dto` package of that service.
5. **Controller** — add the endpoint; use `actorResolver.currentActor()` to identify the user; never accept the userId from the request body.
6. **Exception handler** — no extra work needed; `GlobalExceptionHandler` already covers `ResponseStatusException`, `DataIntegrityViolationException`, and `IllegalArgumentException`.

### If BFF needs to expose the feature publicly

7. **BFF DTO** — mirror the response DTO in `bff.dto.<domain>` (only the fields the client needs).
8. **ServiceClient** — add a method in the corresponding `*ServiceClient` using `GenericRestInvoker`:
   ```java
   ServiceCallResponse<MyDTO> response = invoker.exchange(method, url, body, Map.of(), okType);
   if (response.isOk()) return response.ok();
   throw errorMapper.toException(cfg.getName(), url, response.httpStatus(), response.error());
   ```
9. **Config** — add the new URL to the `*ServiceConfig` `@ConfigurationProperties` class and to `application.yml`.
10. **BFF Controller** — add the endpoint; delegate to the `*ServiceClient`; the `GlobalExceptionHandler` handles all errors automatically.
11. **SecurityConfig (bff-service)** — if the new endpoint must be public (no JWT), add it to `permitAll` matchers. Otherwise nothing to do — `/api/**` already requires authentication.

### Cross-cutting concerns

- **JWT identity**: always extract the actor from `actorResolver.currentActor()`. Never trust userId from the request body.
- **Auth forwarding**: `AuthForwardingInterceptor` (gwent-commons) propagates the Bearer token automatically to all outgoing calls. No manual header passing needed.
- **New service**: if adding a new microservice, it needs: `SecurityConfig` with `oauth2ResourceServer`, `ActorResolver`, `gwent.security.jwt.jwks-uri` in `application.yml`, and a `GlobalExceptionHandler`.

## Known Issues / Gotchas

- `ingame-service` and `solicitud-service` are empty skeletons (only `Application.java`).

## OpenAPI / Swagger

Available at `/swagger-ui/index.html` on each service that has `springdoc-openapi` configured (auth-service, bff-service, jugador-service).
