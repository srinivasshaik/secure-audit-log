# Milestone 3 - Scenario A Chain Verification

## Implemented scope

- `GET /audit/verify`
- Full global-chain traversal in sequence order
- Recalculation of canonical payload and content hash
- Verification of previous-hash linkage and persisted chain state
- Machine-readable first-failure category
- Integration test that changes an event directly in the datastore and confirms detection

## Verification result

An intact response includes `intact: true` and `recordsVerified`. A broken response includes the first affected event when one exists, its sequence, and one of:

- `SEQUENCE_GAP`
- `PREVIOUS_HASH_MISMATCH`
- `CONTENT_HASH_MISMATCH`
- `INVALID_STORED_PAYLOAD`
- `CHAIN_STATE_SEQUENCE_MISMATCH`
- `CHAIN_STATE_HASH_MISMATCH`

Chain state is checked after every event to detect truncation and state-row tampering. State-level violations do not identify an event because the corrupted value belongs to the chain anchor, not an event record.

## API example

```json
GET /audit/verify
{
  "intact": false,
  "recordsVerified": 0,
  "firstInvalidRecordId": "...",
  "firstInvalidSequence": 1,
  "violationType": "CONTENT_HASH_MISMATCH"
}
```
