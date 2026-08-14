# Scenario A Hash Contract

## Timestamp policy

The service assigns `occurredAt` and `ingestedAt` from a UTC `Clock` at append time. Caller-supplied time is deliberately not accepted in Scenario A: an audit timestamp must reflect when this service accepted the event, not an unverified client claim.

## Content canonicalization

Each event stores a SHA-256 `contentHash`. The hashed content is the following exact, ordered field list:

1. Canonicalization version: `audit-log-content-v1`
2. `eventType`
3. `actorId`
4. `resourceType`
5. `resourceId`
6. Canonical JSON payload
7. `occurredAt` as `Instant.toString()` UTC ISO-8601 text

Every value is encoded as UTF-8 and prefixed with its byte length plus `:`. Length-prefixing removes delimiter ambiguity, for example between `ab|c` and `a|bc`.

Payload objects are recursively sorted by property name, serialized without insignificant whitespace, and arrays retain their submitted order. The normalized JSON is both stored and hashed.

## Chain write rule

The service maintains one global chain. The singleton `audit_chain_state` row is acquired with a database pessimistic-write lock within the append transaction. The new event receives:

- `chainSequence`: previous sequence plus one
- `previousHash`: the current chain-state hash, or the 64-zero genesis hash for the first event
- `contentHash`: SHA-256 of its canonical event content

The transaction stores the event and advances chain state to the new sequence/hash. This prevents concurrent requests from creating branches.

## Trade-off

A single global write lock provides a simple, defensible linear chain, at the cost of serialized appends. Partitioned chains can improve throughput, but add verification and export complexity; they are intentionally out of scope for the prototype.
