# Validation and Risk Control

## Reproducible verification

Prerequisites are Java 21 and Docker (required for PostgreSQL Testcontainers evidence). From the repository root run:

```text
./mvnw --batch-mode clean verify
```

On Windows use `mvnw.cmd --batch-mode clean verify`. The command compiles production and test code, applies Flyway migrations, runs unit and H2 integration tests, runs the PostgreSQL compatibility test when Docker is available, and generates coverage.

Stable output locations:

| Evidence | Location |
| --- | --- |
| Per-test XML and text reports | `target/surefire-reports/` |
| JaCoCo HTML report | `target/site/jacoco/index.html` |
| JaCoCo machine-readable report | `target/site/jacoco/jacoco.xml` |
| Packaged application | `target/secure-audit-log-0.0.1-SNAPSHOT.jar` |

Generated reports are intentionally ignored because they contain machine paths and become stale. CI regenerates them from the reviewed revision. A Docker-disabled local skip must be reported as a limitation and is not PostgreSQL evidence.

## Test strategy and risk coverage

- Unit tests pin canonical JSON, field-boundary-safe hashing, and rate-limit failure behavior.
- H2 integration tests exercise migrations, tamper detection, archival, redaction, export, reporting, security failures, and concurrent append invariants.
- PostgreSQL Testcontainers validates dialect/migration compatibility and persisted hash verification.
- Security tests cover unauthenticated access and protected development tooling. Production JWT/TLS configuration is a deployment gate and must also be exercised against the target issuer in pre-production.

The concurrency invariant is stronger than request success: committed sequence values must be unique and contiguous and the complete chain must verify. Known residual production controls are external identity-provider policy, tenant-partitioned authorization, TLS certificate/proxy configuration, secret rotation, database encryption/backups, distributed rate limiting, alerting, and load testing.

## Latest local result

On 2026-08-15, `mvn --batch-mode clean verify` completed successfully with no test failures or errors. The Docker-dependent PostgreSQL Testcontainers test was skipped locally and must run in CI. JaCoCo generated current line, branch, and instruction coverage reports under `target/site/jacoco/`.

The added cases cover invalid and duplicate redaction paths, nested arrays and escaped JSON Pointer tokens, missing/container/invalid-index targets, repeated and missing-event redactions, full query filtering and archived visibility, non-object payload rejection, previous-hash and chain-state corruption, authenticated event creation, HTTP Bean Validation, and problem-detail error translation.

