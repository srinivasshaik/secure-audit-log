package com.secure.auditlog.audit.application;

import java.time.Instant;

public record AuditEventQuery(
		String actorId,
		String resourceType,
		String resourceId,
		String eventType,
		Instant from,
		Instant to,
		boolean includeArchived,
		int page,
		int size) {
}
