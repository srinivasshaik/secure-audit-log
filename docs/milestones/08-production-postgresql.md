# Milestone 8 - Production PostgreSQL Profile

## Profile boundary

- `dev` (default): H2 in PostgreSQL compatibility mode; intended for all local development.
- `prod`: PostgreSQL only; requires database and API credentials from environment variables.

## Production startup

1. Copy `.env.example` to `.env` and replace every sample password.
2. Run `docker compose --profile prod up --build`.
3. Flyway applies the same versioned migrations against PostgreSQL before the application accepts requests.

The Docker image uses Java 21. H2 is never included as the active runtime datasource under the `prod` profile.

## Scope boundary

This milestone deliberately does not add Testcontainers or CI. H2 remains the development database throughout the project, per the approved scope.
