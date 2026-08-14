package com.secure.auditlog.audit.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.secure.auditlog.audit.domain.AuditEventContent;
import com.secure.auditlog.audit.domain.AuditHashingService;
import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.domain.ChainVerificationResult;
import com.secure.auditlog.audit.domain.ChainViolationType;
import com.secure.auditlog.audit.infrastructure.AuditChainStateEntity;
import com.secure.auditlog.audit.infrastructure.AuditChainStateJpaRepository;
import com.secure.auditlog.audit.infrastructure.AuditEventEntity;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;
import com.secure.auditlog.redaction.AuditEventRedactionEntity;
import com.secure.auditlog.redaction.AuditEventRedactionRepository;
import com.secure.auditlog.redaction.AuditRedactionHashingService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditChainVerificationService {

	public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditChainStateJpaRepository chainStateRepository;
	private final CanonicalJsonService canonicalJsonService;
	private final AuditHashingService hashingService;
	private final AuditEventRedactionRepository redactionRepository;
	private final AuditRedactionHashingService redactionHashingService;

	public AuditChainVerificationService(AuditEventJpaRepository auditEventRepository,
			AuditChainStateJpaRepository chainStateRepository, CanonicalJsonService canonicalJsonService,
			AuditHashingService hashingService, AuditEventRedactionRepository redactionRepository,
			AuditRedactionHashingService redactionHashingService) {
		this.auditEventRepository = auditEventRepository;
		this.chainStateRepository = chainStateRepository;
		this.canonicalJsonService = canonicalJsonService;
		this.hashingService = hashingService;
		this.redactionRepository = redactionRepository;
		this.redactionHashingService = redactionHashingService;
	}

	@Transactional(readOnly = true)
	public ChainVerificationResult verify() {
		List<AuditEventEntity> events = auditEventRepository.findAllByOrderByChainSequenceAsc();
		Map<java.util.UUID, AuditEventRedactionEntity> redactions = events.isEmpty() ? Map.of() : redactionRepository
				.findAllByAuditEventIdIn(events.stream().map(AuditEventEntity::getId).toList()).stream()
				.collect(java.util.stream.Collectors.toMap(AuditEventRedactionEntity::getAuditEventId, Function.identity()));
		String expectedPreviousHash = GENESIS_HASH;
		long expectedSequence = 1;

		for (AuditEventEntity event : events) {
			if (event.getChainSequence() != expectedSequence) {
				return ChainVerificationResult.broken(expectedSequence - 1, event.getId(), event.getChainSequence(),
						ChainViolationType.SEQUENCE_GAP);
			}
			if (!expectedPreviousHash.equals(event.getPreviousHash())) {
				return ChainVerificationResult.broken(expectedSequence - 1, event.getId(), event.getChainSequence(),
						ChainViolationType.PREVIOUS_HASH_MISMATCH);
			}

			String canonicalPayload;
			try {
				canonicalPayload = canonicalJsonService.canonicalizeStoredPayload(event.getPayload());
			} catch (IllegalArgumentException | IllegalStateException exception) {
				return ChainVerificationResult.broken(expectedSequence - 1, event.getId(), event.getChainSequence(),
						ChainViolationType.INVALID_STORED_PAYLOAD);
			}
			AuditEventRedactionEntity redaction = redactions.get(event.getId());
			if (redaction != null) {
				String recomputedRedactionHash = redactionHashingService.redactionHash(redaction.getOriginalContentHash(), canonicalPayload,
						redaction.getRedactedPaths(), redaction.getRedactedAt());
				if (!canonicalPayload.equals(redaction.getRedactedPayload())
						|| !event.getContentHash().equals(redaction.getOriginalContentHash())
						|| !recomputedRedactionHash.equals(redaction.getRedactionHash())) {
					return ChainVerificationResult.broken(expectedSequence - 1, event.getId(), event.getChainSequence(),
								ChainViolationType.REDACTION_EVIDENCE_MISMATCH);
				}
			} else {
				String recomputedContentHash = hashingService.contentHash(new AuditEventContent(event.getEventType(), event.getActorId(),
						event.getResourceType(), event.getResourceId(), canonicalPayload, event.getOccurredAt()));
				if (!recomputedContentHash.equals(event.getContentHash())) {
					return ChainVerificationResult.broken(expectedSequence - 1, event.getId(), event.getChainSequence(),
							ChainViolationType.CONTENT_HASH_MISMATCH);
				}
			}

			expectedPreviousHash = event.getContentHash();
			expectedSequence++;
		}

		AuditChainStateEntity state = chainStateRepository.findById(1L)
				.orElseThrow(() -> new IllegalStateException("Audit chain state is missing"));
		long verifiedCount = expectedSequence - 1;
		if (state.getLastSequence() != verifiedCount) {
			return ChainVerificationResult.broken(verifiedCount, null, state.getLastSequence(),
					ChainViolationType.CHAIN_STATE_SEQUENCE_MISMATCH);
		}
		if (!state.getLastHash().equals(expectedPreviousHash)) {
			return ChainVerificationResult.broken(verifiedCount, null, state.getLastSequence(),
					ChainViolationType.CHAIN_STATE_HASH_MISMATCH);
		}
		return ChainVerificationResult.intact(verifiedCount);
	}
}
