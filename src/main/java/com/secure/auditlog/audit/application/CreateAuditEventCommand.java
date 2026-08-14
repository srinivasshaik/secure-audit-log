package com.secure.auditlog.audit.application;

import tools.jackson.databind.JsonNode;

public record CreateAuditEventCommand(
		String eventType,
		String actorId,
		String resourceType,
		String resourceId,
		JsonNode payload) {
}
