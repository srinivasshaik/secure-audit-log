# Redaction and Export Design

## Redaction scheme

Redaction is a one-time, authorized exception to normal append-only behavior:

1. The service replaces the selected scalar JSON Pointer values with `[REDACTED]` in the event row, removing the original sensitive value from the online audit record.
2. It preserves the original `contentHash` and chain links.
3. It stores a redaction certificate containing the canonical redacted payload, sorted JSON Pointer paths, redaction timestamp, original content hash, and a SHA-256 `redactionHash` over those values.

During verification, unredacted events have their content hash recalculated normally. Redacted events instead require the persisted payload to match the certified redacted payload, the certificate's original hash to match the event's content hash, and the certificate hash to recalculate correctly.

This preserves evidence of the original event without retaining its redacted scalar values. Redaction is intentionally one-time to retain a clear custody trail.

### Limitations

- This prototype has no role-based authorization yet; the redaction endpoint must be restricted in the security milestone.
- It supports scalar JSON Pointer targets only, preventing accidental removal of entire nested structures.
- Database backups and downstream copies require their own retention/redaction processes.

## Export bundle

`GET /audit/exports?actorId=...` or `?resourceId=...` creates an `audit-export-v1` JSON bundle. It includes selected event contents, sequence, predecessor hash, content hash, any redaction certificate, the chain head at export, and a deterministic `exportHash`.

A recipient can recompute every unredacted content hash, validate any redaction certificate, check the bundle hash, and compare preserved sequence/predecessor values. The bundle is not digitally signed; therefore it demonstrates internal consistency, not the sender's identity. A production design should sign the export manifest with a managed private key and publish the public verification key.
