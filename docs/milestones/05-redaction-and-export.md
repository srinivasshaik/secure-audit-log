# Milestone 5 - Scenario B Redaction and Export

## Implemented scope

- One-time, structured JSON Pointer redaction endpoint
- Tamper-evident redaction certificates
- Chain verification aware of redacted events
- Bulk export by exactly one actor ID or resource ID
- Self-contained export manifest with event chain metadata and deterministic bundle hash
- Integration coverage for redaction and export

## API examples

```json
POST /audit/events/{eventId}/redactions
{ "paths": ["/accountNumber", "/customer/ssn"] }
```

```text
GET /audit/exports?actorId=user-123
GET /audit/exports?resourceId=account-456
```
