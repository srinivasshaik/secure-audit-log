package com.secure.auditlog.audit.domain;

import java.time.Instant;

public record AuditEventContent(
		String eventType,
		String actorId,
		String resourceType,
		String resourceId,
		String canonicalPayload,
		Instant occurredAt) {
}
