package com.secure.auditlog.export;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record AuditExportEvent(UUID id, long chainSequence, String eventType, String actorId, String resourceType,
		String resourceId, JsonNode payload, Instant occurredAt, String previousHash, String contentHash,
		AuditExportRedaction redaction) {
}
