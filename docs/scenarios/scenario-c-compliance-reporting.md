# Scenario C - Compliance Reporting

## Original product statement

> Regulators need to be able to audit access to client account data.

## Clarification questions

1. Which resources are client accounts, and which event types constitute data access rather than account administration?
2. Which regulator roles may run reports, for which jurisdictions, and with what approval/audit trail?
3. What time range, retention period, export format, and report latency are required?
4. Must the report include archived/redacted records and how should privacy restrictions apply?
5. Is a signed report required, or is live service integrity verification sufficient?

## Assumptions for this prototype

- `resourceType=ACCOUNT` represents a client account.
- `ACCOUNT_ACCESSED`, `ACCOUNT_VIEWED`, and `ACCOUNT_EXPORTED` mean access to client account data.
- Compliance users need a time-bounded, paginated report, optionally narrowed by actor or account ID.
- Archived records must be reportable; the report therefore includes them.
- The report includes current full-chain verification so a reviewer can see the ledger's integrity state.

## Normalized requirement

An authorized compliance user can request a paginated report of client-account access events for a mandatory UTC time range, optionally filtered by actor and account. The response includes event fields, hash metadata, redaction state, and full-chain integrity status at report time.

## Implemented endpoint

```text
GET /compliance/client-account-access?from=2026-01-01T00:00:00Z&to=2026-01-31T23:59:59Z&actorId=user-123&resourceId=account-456&page=0&size=50
```

## Explicit scope boundary

This implementation does not yet enforce regulator authorization, jurisdictional entitlements, report signing, asynchronous large-report generation, or regulatory record-layout requirements. These need confirmed product/legal requirements and are addressed as production-hardening follow-ups rather than guessed behavior.
