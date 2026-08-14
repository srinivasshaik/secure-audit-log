package com.secure.auditlog.compliance;

import java.time.Instant;
import java.util.List;

import com.secure.auditlog.audit.api.AuditChainVerificationResponse;
import com.secure.auditlog.audit.api.AuditEventPageResponse;
import com.secure.auditlog.audit.api.AuditEventResponse;
import com.secure.auditlog.audit.api.AuditEventResponseMapper;
import com.secure.auditlog.audit.application.AuditChainVerificationService;
import com.secure.auditlog.audit.application.InvalidAuditEventException;
import com.secure.auditlog.audit.domain.ChainVerificationResult;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceReportingService {

	private static final List<String> CLIENT_ACCOUNT_ACCESS_EVENTS = List.of(
			"ACCOUNT_ACCESSED", "ACCOUNT_VIEWED", "ACCOUNT_EXPORTED");
	private static final int MAX_PAGE_SIZE = 200;

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditEventResponseMapper responseMapper;
	private final AuditChainVerificationService verificationService;

	public ComplianceReportingService(AuditEventJpaRepository auditEventRepository, AuditEventResponseMapper responseMapper,
			AuditChainVerificationService verificationService) {
		this.auditEventRepository = auditEventRepository;
		this.responseMapper = responseMapper;
		this.verificationService = verificationService;
	}

	@Transactional(readOnly = true)
	public ClientAccountAccessReport report(String actorId, String resourceId, Instant from, Instant to, int page, int size) {
		validate(from, to, page, size);
		Specification<AuditEventEntity> specification = (root, query, builder) -> {
			var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
			predicates.add(builder.equal(root.get("resourceType"), "ACCOUNT"));
			predicates.add(root.get("eventType").in(CLIENT_ACCOUNT_ACCESS_EVENTS));
			predicates.add(builder.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), from));
			predicates.add(builder.lessThanOrEqualTo(root.<Instant>get("occurredAt"), to));
			if (actorId != null) predicates.add(builder.equal(root.get("actorId"), actorId));
			if (resourceId != null) predicates.add(builder.equal(root.get("resourceId"), resourceId));
			return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
		Page<AuditEventResponse> result = auditEventRepository.findAll(specification,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("chainSequence"))))
				.map(responseMapper::toResponse);
		AuditEventPageResponse events = new AuditEventPageResponse(result.getContent(), result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages());
		ChainVerificationResult verification = verificationService.verify();
		return new ClientAccountAccessReport(events, new AuditChainVerificationResponse(verification.intact(),
				verification.recordsVerified(), verification.firstInvalidRecordId(), verification.firstInvalidSequence(),
				verification.violationType()));
	}

	private void validate(Instant from, Instant to, int page, int size) {
		if (from == null || to == null) throw new InvalidAuditEventException("from and to are required");
		if (from.isAfter(to)) throw new InvalidAuditEventException("from must not be after to");
		if (page < 0) throw new InvalidAuditEventException("page must be zero or greater");
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidAuditEventException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
	}
}
