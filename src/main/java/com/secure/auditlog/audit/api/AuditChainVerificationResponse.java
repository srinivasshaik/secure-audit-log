package com.secure.auditlog.audit.api;

import java.util.UUID;

import com.secure.auditlog.audit.domain.ChainViolationType;

public record AuditChainVerificationResponse(
		boolean intact,
		long recordsVerified,
		UUID firstInvalidRecordId,
		Long firstInvalidSequence,
		ChainViolationType violationType) {
}
