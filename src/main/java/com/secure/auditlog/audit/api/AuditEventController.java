package com.secure.auditlog.audit.api;

import java.net.URI;
import java.time.Instant;

import com.secure.auditlog.audit.application.AuditEventQuery;
import com.secure.auditlog.audit.application.AuditLogService;
import com.secure.auditlog.audit.application.CreateAuditEventCommand;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/events")
public class AuditEventController {

	private final AuditLogService auditLogService;
	private final AuditEventResponseMapper responseMapper;

	public AuditEventController(AuditLogService auditLogService, AuditEventResponseMapper responseMapper) {
		this.auditLogService = auditLogService;
		this.responseMapper = responseMapper;
	}

	@PostMapping
	public ResponseEntity<AuditEventResponse> append(@Valid @RequestBody CreateAuditEventRequest request) {
		AuditEventResponse response = responseMapper.toResponse(auditLogService.append(new CreateAuditEventCommand(
				request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), request.payload())));
		return ResponseEntity.created(URI.create("/audit/events/" + response.id())).body(response);
	}

	@GetMapping
	public AuditEventPageResponse query(
			@RequestParam(required = false) String actorId,
			@RequestParam(required = false) String resourceType,
			@RequestParam(required = false) String resourceId,
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		Page<AuditEventResponse> result = auditLogService.query(
				new AuditEventQuery(actorId, resourceType, resourceId, eventType, from, to, page, size))
				.map(responseMapper::toResponse);
		return new AuditEventPageResponse(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}
}
