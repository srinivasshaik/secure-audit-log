package com.secure.auditlog.audit.api;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record AuditEventResponse(
		UUID id,
		long chainSequence,
		String eventType,
		String actorId,
		String resourceType,
		String resourceId,
		JsonNode payload,
		Instant occurredAt,
		Instant ingestedAt,
		Instant archivedAt,
		String previousHash,
		String contentHash) {
}
