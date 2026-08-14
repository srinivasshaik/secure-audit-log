package com.secure.auditlog.export;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.secure.auditlog.audit.application.InvalidAuditEventException;
import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.infrastructure.AuditChainStateEntity;
import com.secure.auditlog.audit.infrastructure.AuditChainStateJpaRepository;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;
import com.secure.auditlog.redaction.AuditEventRedactionEntity;
import com.secure.auditlog.redaction.AuditEventRedactionRepository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditExportService {

	private static final String FORMAT_VERSION = "audit-export-v1";

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditEventRedactionRepository redactionRepository;
	private final AuditChainStateJpaRepository chainStateRepository;
	private final CanonicalJsonService canonicalJsonService;
	private final AuditExportHashingService exportHashingService;
	private final Clock clock;

	public AuditExportService(AuditEventJpaRepository auditEventRepository, AuditEventRedactionRepository redactionRepository,
			AuditChainStateJpaRepository chainStateRepository, CanonicalJsonService canonicalJsonService,
			AuditExportHashingService exportHashingService, Clock clock) {
		this.auditEventRepository = auditEventRepository;
		this.redactionRepository = redactionRepository;
		this.chainStateRepository = chainStateRepository;
		this.canonicalJsonService = canonicalJsonService;
		this.exportHashingService = exportHashingService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AuditExportBundle export(String actorId, String resourceId) {
		if ((actorId == null) == (resourceId == null)) {
			throw new InvalidAuditEventException("exactly one of actorId or resourceId is required");
		}
		String selectorType = actorId != null ? "actorId" : "resourceId";
		String selectorValue = actorId != null ? actorId : resourceId;
		Specification<AuditEventEntity> specification = (root, query, builder) ->
				builder.equal(root.get(selectorType.equals("actorId") ? "actorId" : "resourceId"), selectorValue);
		List<AuditEventEntity> events = auditEventRepository.findAll(specification, Sort.by("chainSequence").ascending());
		Map<UUID, AuditEventRedactionEntity> redactions = events.isEmpty() ? Map.of() : redactionRepository
				.findAllByAuditEventIdIn(events.stream().map(AuditEventEntity::getId).toList()).stream()
				.collect(java.util.stream.Collectors.toMap(AuditEventRedactionEntity::getAuditEventId, Function.identity()));
		List<AuditExportEvent> exportEvents = events.stream().map(event -> toExportEvent(event, redactions.get(event.getId()))).toList();
		AuditChainStateEntity state = chainStateRepository.findById(1L)
				.orElseThrow(() -> new IllegalStateException("Audit chain state is missing"));
		Instant exportedAt = clock.instant();
		String exportHash = exportHashingService.hash(FORMAT_VERSION, exportedAt.toString(), selectorType, selectorValue,
				state.getLastSequence(), state.getLastHash(), exportEvents);
		return new AuditExportBundle(FORMAT_VERSION, exportedAt, selectorType, selectorValue, state.getLastSequence(),
				state.getLastHash(), exportEvents, exportHash);
	}

	private AuditExportEvent toExportEvent(AuditEventEntity event, AuditEventRedactionEntity redaction) {
		AuditExportRedaction exportRedaction = redaction == null ? null : new AuditExportRedaction(redaction.getRedactedPaths(),
				redaction.getRedactedAt(), redaction.getOriginalContentHash(), redaction.getRedactionHash());
		return new AuditExportEvent(event.getId(), event.getChainSequence(), event.getEventType(), event.getActorId(),
				event.getResourceType(), event.getResourceId(), canonicalJsonService.read(event.getPayload()), event.getOccurredAt(),
				event.getPreviousHash(), event.getContentHash(), exportRedaction);
	}
}
