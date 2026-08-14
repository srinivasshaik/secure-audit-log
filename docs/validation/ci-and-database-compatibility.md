# CI and Database Compatibility

## Test layers

- Unit tests cover canonical JSON and SHA-256 hashing without a database.
- Standard Spring integration tests use the default `dev` profile and H2 in PostgreSQL compatibility mode. This remains the fast, daily-development path.
- `PostgreSqlCompatibilityTests` starts PostgreSQL 17 with Testcontainers, applies the real Flyway migrations, appends an event, and verifies its chain. It detects database-engine differences that H2 mode cannot guarantee.

The Testcontainers class is skipped when Docker is unavailable locally. GitHub Actions runs it on Ubuntu where Docker is available; a failure is a delivery blocker.

## CI gate

`.github/workflows/ci.yml` runs `mvn --batch-mode verify` for pull requests and pushes to `main`, using Temurin Java 21 and Maven dependency caching. The workflow has read-only repository permissions and uses no application credentials. PostgreSQL test credentials exist only inside the disposable container.

## Interview discussion

**Why not use PostgreSQL for every developer test?** H2 provides a faster feedback loop and is the intentional development database. The Testcontainers layer gives targeted assurance that migrations and persistence-sensitive integrity behavior work with the production engine.
