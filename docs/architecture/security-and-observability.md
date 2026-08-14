# Security and Observability

## Security boundary

The prototype uses stateless HTTP Basic authentication to make caller identity and role separation explicit without inventing an identity-provider integration. In production, replace the in-memory user with enterprise OIDC/OAuth2 resource-server validation and map approved identity-provider claims to these roles:

- `AUDIT_READER` - query, verification, and API documentation
- `AUDIT_WRITER` - append events
- `AUDIT_PRIVACY_OFFICER` - redact payload values
- `COMPLIANCE_OFFICER` - compliance reporting
- `SYSTEM_ADMIN` - local H2 console only

Health and info are unauthenticated for orchestrator probes. Prometheus metrics require authentication. The default H2 profile has an explicitly local-only fallback password; PostgreSQL requires `AUDIT_LOG_API_PASSWORD`.

## Logging and monitoring

- Every request accepts or generates `X-Correlation-ID`, returns it in the response, and places it in MDC-backed logs.
- Request logs contain only method, path, status, and duration. Bodies, authorization headers, payloads, and sensitive values are never logged.
- Actuator exposes health, info, and Prometheus metrics. Standard HTTP metrics are emitted by the framework.

## Known limits

This milestone does not add rate limiting, TLS termination, distributed tracing export, audit-event authorization by tenant, secret-manager integration, or immutable database permissions. These require deployment and identity context that the assignment does not supply.
