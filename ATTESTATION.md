# Individual Work Attestation

| Field | Attested value |
| --- | --- |
| Full name | Srinivas Shaik |
| Email | srinivasshaik16@gmail.com |
| Assignment | Build an AI-Assisted Software Engineering System — Audit Log Service |
| Work started | 2026-08-14 |
| Submission reviewed | 2026-08-15 (Asia/Calcutta) |
| Repository / branch | `secure-audit-log` / `main` |
| Reviewed baseline | `34a4120` plus the documented delivery working tree |
| Delivery revision | The commit containing this attestation; verify with `git log -1 --format=%H` after commit |

I, Srinivas Shaik, attest that this submission is my own individual work, completed on my own machine and accounts, and that it honestly reflects my development process and use of AI. I reviewed the source changes, tests, generated reports, and statements below before authorizing the delivery commit and push.

## Scope and boundaries

Included in this attestation are the Java source, configuration, Flyway migrations, automated tests, Maven build and reporting configuration, CI workflow, container/development configuration, and documentation tracked by the delivery revision.

Excluded are generated `target/` files, IDE metadata, local environment variables and secrets, external identity-provider configuration, deployed infrastructure, and GitHub-hosted run results. Generated test and coverage reports are reproducible evidence, not source-controlled claims; commands and stable report locations are documented in `docs/validation/README.md`.

The service demonstrates application controls and deployment requirements. It does not claim that TLS termination, an external identity provider, secret rotation, database encryption, backups, or monitoring are operational until those controls are configured and verified in the target environment.

## Claim-to-evidence mapping

| Claim | Primary implementation evidence | Validation evidence |
| --- | --- | --- |
| Audit entries form a deterministic SHA-256 chain | `AuditHashingService`, `CanonicalJsonService`, `AuditLogService`, `docs/architecture/hash-contract.md` | `AuditHashingServiceTests`, `CanonicalJsonServiceTests`, `AuditChainVerificationIntegrationTests` |
| Direct datastore mutation is detected | `AuditChainVerificationService` | `AuditChainVerificationIntegrationTests#detectsDirectDatastoreModification` |
| Concurrent appends serialize chain state | Pessimistic chain-state locking in `AuditChainStateJpaRepository` and the append transaction | Concurrent append integration test and PostgreSQL compatibility test |
| API authentication and role authorization are enforced | `SecurityConfiguration`, production OAuth2 resource-server configuration, endpoint role rules | Security integration and production-profile tests |
| Development consoles and API docs are not anonymously exposed | Profile configuration and security rules | Unauthenticated console/OpenAPI tests |
| Production requires externally supplied identity, database, and transport settings | `application-prod.yaml` with fail-fast placeholders and HTTPS enforcement | Production context/configuration tests |
| Tests and coverage are reproducible | Maven Surefire and JaCoCo configuration, GitHub Actions workflow | `mvn clean verify`; reports under `target/surefire-reports` and `target/site/jacoco` |

## AI usage and review

AI assistance is disclosed in `docs/ai-usage/usage-log.md`. Before the delivery commit and push, I will review that log, the complete Git diff, dependency/configuration changes, and the result of `mvn clean verify`. No production credentials, customer records, tokens, or other confidential data are included in this repository or prompts recorded by the project.

## Reviewer reproduction

From a clean checkout with Java 21 and Docker available, run `./mvnw --batch-mode clean verify`. Compare the checked-out revision with the delivery revision above, then inspect the report locations listed in `docs/validation/README.md`. A skipped Docker-backed test is not evidence of PostgreSQL compatibility; CI must execute it with Docker available.
