package com.secure.auditlog.redaction;

import java.time.Instant;
import java.util.UUID;

public record AuditRedactionResponse(UUID eventId, Instant redactedAt, String redactedPaths, String redactionHash) {
}
