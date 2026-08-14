package com.secure.auditlog.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import com.secure.auditlog.audit.domain.AuditEventContent;
import com.secure.auditlog.audit.domain.AuditHashingService;
import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.infrastructure.AuditChainStateEntity;
import com.secure.auditlog.audit.infrastructure.AuditChainStateJpaRepository;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditLogService {

	private static final int MAX_PAGE_SIZE = 200;

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditChainStateJpaRepository chainStateRepository;
	private final CanonicalJsonService canonicalJsonService;
	private final AuditHashingService hashingService;
	private final Clock clock;

	public AuditLogService(AuditEventJpaRepository auditEventRepository,
			AuditChainStateJpaRepository chainStateRepository, CanonicalJsonService canonicalJsonService,
			AuditHashingService hashingService, Clock clock) {
		this.auditEventRepository = auditEventRepository;
		this.chainStateRepository = chainStateRepository;
		this.canonicalJsonService = canonicalJsonService;
		this.hashingService = hashingService;
		this.clock = clock;
	}

	@Transactional
	public AuditEventEntity append(CreateAuditEventCommand command) {
		String canonicalPayload;
		try {
			canonicalPayload = canonicalJsonService.canonicalizeObject(command.payload());
		} catch (IllegalArgumentException exception) {
			throw new InvalidAuditEventException(exception.getMessage());
		}

		AuditChainStateEntity chainState = chainStateRepository.findSingletonForUpdate()
				.orElseThrow(() -> new IllegalStateException("Audit chain state is missing"));
		// PostgreSQL stores TIMESTAMP values at microsecond precision by default.
		// Hash the persisted representation so a later verification reads identical data.
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		String contentHash = hashingService.contentHash(new AuditEventContent(
				command.eventType(), command.actorId(), command.resourceType(), command.resourceId(), canonicalPayload, now));
		String previousHash = chainState.getLastHash();
		long sequence = chainState.nextSequence();

		AuditEventEntity event = new AuditEventEntity(UUID.randomUUID(), sequence, command.eventType(), command.actorId(),
				command.resourceType(), command.resourceId(), canonicalPayload, now, now, previousHash, contentHash);
		auditEventRepository.save(event);
		chainState.setLastHash(contentHash);
		return event;
	}

	@Transactional(readOnly = true)
	public Page<AuditEventEntity> query(AuditEventQuery query) {
		validateQuery(query);
		PageRequest pageRequest = PageRequest.of(query.page(), query.size(),
				Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("chainSequence")));
		return auditEventRepository.findAll(toSpecification(query), pageRequest);
	}

	private void validateQuery(AuditEventQuery query) {
		if (query.page() < 0) {
			throw new InvalidAuditEventException("page must be zero or greater");
		}
		if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
			throw new InvalidAuditEventException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
			throw new InvalidAuditEventException("from must not be after to");
		}
	}

	private Specification<AuditEventEntity> toSpecification(AuditEventQuery query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			var predicates = new ArrayList<Predicate>();
			if (query.actorId() != null) predicates.add(criteriaBuilder.equal(root.get("actorId"), query.actorId()));
			if (query.resourceType() != null) predicates.add(criteriaBuilder.equal(root.get("resourceType"), query.resourceType()));
			if (query.resourceId() != null) predicates.add(criteriaBuilder.equal(root.get("resourceId"), query.resourceId()));
			if (query.eventType() != null) predicates.add(criteriaBuilder.equal(root.get("eventType"), query.eventType()));
			if (query.from() != null) predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), query.from()));
			if (query.to() != null) predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Instant>get("occurredAt"), query.to()));
			if (!query.includeArchived()) predicates.add(criteriaBuilder.isNull(root.get("archivedAt")));
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
