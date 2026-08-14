package com.secure.auditlog.audit.api;

import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;

import org.springframework.stereotype.Component;

@Component
public class AuditEventResponseMapper {

	private final CanonicalJsonService canonicalJsonService;

	public AuditEventResponseMapper(CanonicalJsonService canonicalJsonService) {
		this.canonicalJsonService = canonicalJsonService;
	}

	public AuditEventResponse toResponse(AuditEventEntity event) {
		return new AuditEventResponse(event.getId(), event.getChainSequence(), event.getEventType(), event.getActorId(),
				event.getResourceType(), event.getResourceId(), canonicalJsonService.read(event.getPayload()), event.getOccurredAt(),
				event.getIngestedAt(), event.getArchivedAt(), event.getPreviousHash(), event.getContentHash());
	}
}
