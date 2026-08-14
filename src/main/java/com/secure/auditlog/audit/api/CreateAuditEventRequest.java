package com.secure.auditlog.audit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import tools.jackson.databind.JsonNode;

public record CreateAuditEventRequest(
		@NotBlank @Size(max = 100) String eventType,
		@NotBlank @Size(max = 255) String actorId,
		@NotBlank @Size(max = 100) String resourceType,
		@NotBlank @Size(max = 255) String resourceId,
		@NotNull JsonNode payload) {
}
