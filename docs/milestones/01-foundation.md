# Milestone 1 - Foundation

## Scope

- Java 21 and Spring Boot 4.1.0 baseline
- H2 as the development runtime database
- PostgreSQL production profile and Docker Compose service
- Flyway as the sole schema-management mechanism
- JPA configuration with Hibernate schema generation disabled
- Actuator health endpoint and Swagger UI dependency

## Decisions

- H2 uses PostgreSQL compatibility mode to catch basic portability concerns early. It is not treated as a substitute for PostgreSQL integration testing.
- The production PostgreSQL profile is opt-in. Its credentials come from environment variables and are intentionally absent from version control.
- Flyway owns all schema changes. Hibernate runs in `validate` mode so application startup cannot silently alter an audit ledger.
- The first migration is an intentional baseline. Scenario A will add the first audit schema migration.

## How to run

Use the H2 development profile (the default):

```shell
# Windows PowerShell
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

The H2 console is available locally at `/h2-console`. Swagger UI is available at `/swagger-ui.html` once the application is running.

To start the production PostgreSQL service and application:

```shell
docker compose --profile prod up --build
```

The production profile is `prod` and requires `AUDIT_LOG_DB_*` and `AUDIT_LOG_API_PASSWORD` environment variables. Docker Compose supplies the database variables from `.env`.

## Deferred

No audit entity, API, scheduler, security policy, or hash-chain behavior is implemented in this milestone.
