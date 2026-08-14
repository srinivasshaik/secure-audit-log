# Retention Design

## Chosen approach: soft archival

At the configured retention cutoff, the scheduler marks eligible events with `archivedAt`. It does not delete or alter the immutable audit fields, payload, hashes, sequence, or predecessor reference.

This meets the assignment's archive-or-soft-delete option while preserving the complete chain. Normal queries exclude archived events by default; callers can include them with `includeArchived=true`. The chain-verification endpoint always checks every event, including archived rows.

## Why archival metadata is not hashed

`archivedAt` is controlled lifecycle metadata created after the original event. Including it in the content hash would make a legitimate policy-driven archival operation look like tampering. The protected content remains the event data defined by the Scenario A hash contract.

## Operational safeguards

- Retention duration and schedule are configuration properties, not code constants.
- The update targets only non-archived events older than the cutoff, making repeated scheduler runs idempotent.
- The default one-year retention is a placeholder; a real policy needs legal, compliance, and data-owner approval.
- Physical deletion, cold-storage export, and archival authorization are deferred to Scenario B export/security work.
