package com.secure.auditlog.redaction;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/events")
public class AuditRedactionController {

	private final AuditRedactionService redactionService;

	public AuditRedactionController(AuditRedactionService redactionService) {
		this.redactionService = redactionService;
	}

	@PostMapping("/{eventId}/redactions")
	public ResponseEntity<AuditRedactionResponse> redact(@PathVariable UUID eventId,
			@Valid @RequestBody RedactAuditEventRequest request) {
		AuditEventRedactionEntity redaction = redactionService.redact(eventId, request.paths());
		AuditRedactionResponse response = new AuditRedactionResponse(redaction.getAuditEventId(), redaction.getRedactedAt(),
				redaction.getRedactedPaths(), redaction.getRedactionHash());
		return ResponseEntity.created(URI.create("/audit/events/" + eventId + "/redactions")).body(response);
	}
}
