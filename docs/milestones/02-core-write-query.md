# Milestone 2 - Scenario A Write and Query

## Implemented scope

- `POST /audit/events` append API
- Server-assigned timestamps
- Deterministic JSON normalization and SHA-256 content hash
- Transactional, single-chain sequencing using a pessimistic database lock
- `GET /audit/events` filtering by actor, resource type/id, event type, and time range
- Bounded offset pagination (maximum page size: 200)
- Flyway V2 schema migration and unit tests for hash/JSON behavior

## Deferred to Milestone 3

- `GET /audit/verify`
- Verification failure classification
- Direct datastore-tampering integration test

## API examples

```json
POST /audit/events
{
  "eventType": "RECORD_UPDATED",
  "actorId": "user-123",
  "resourceType": "ACCOUNT",
  "resourceId": "account-456",
  "payload": { "changedFields": ["status"], "channel": "web" }
}
```

```text
GET /audit/events?actorId=user-123&resourceType=ACCOUNT&resourceId=account-456&page=0&size=50
```
