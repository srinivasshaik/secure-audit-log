# Milestone 1 - Foundation

## Scope

- Java 21 and Spring Boot 4.1.0 baseline
- H2 as the default local runtime database
- Dormant PostgreSQL profile and optional Docker Compose service
- Flyway as the sole schema-management mechanism
- JPA configuration with Hibernate schema generation disabled
- Actuator health endpoint and Swagger UI dependency

## Decisions

- H2 uses PostgreSQL compatibility mode to catch basic portability concerns early. It is not treated as a substitute for PostgreSQL integration testing.
- The PostgreSQL profile is opt-in. Its credentials come from environment variables and are intentionally absent from version control.
- Flyway owns all schema changes. Hibernate runs in `validate` mode so application startup cannot silently alter an audit ledger.
- The first migration is an intentional baseline. Scenario A will add the first audit schema migration.

## How to run

Use H2 by default:

```shell
# Windows PowerShell
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

The H2 console is available locally at `/h2-console`. Swagger UI is available at `/swagger-ui.html` once the application is running.

To prepare the optional PostgreSQL container for a later milestone:

```shell
docker compose --profile postgres up -d
```

Then start the application with `SPRING_PROFILES_ACTIVE=postgres` and set the `AUDIT_LOG_DB_*` environment variables.

## Deferred

No audit entity, API, scheduler, security policy, or hash-chain behavior is implemented in this milestone.
