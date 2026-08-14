# Milestone 7 - Security and Observability

## Implemented scope

- Stateless HTTP Basic authentication and role-based endpoint authorization
- Development versus production PostgreSQL password configuration
- Swagger/OpenAPI Basic-auth documentation
- Correlation ID propagation and safe request-completion logs
- Actuator health/info/Prometheus exposure
- Security integration tests for public health, authentication enforcement, and authorized access

## Local development

For the `dev` H2 profile only, the default development username is `audit-service` and the fallback password is `local-dev-only-change-me`. Override it with `AUDIT_LOG_API_PASSWORD`; never use the fallback outside local development.
