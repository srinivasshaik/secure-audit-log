package com.secure.auditlog.audit.domain;

import java.util.UUID;

public record ChainVerificationResult(
		boolean intact,
		long recordsVerified,
		UUID firstInvalidRecordId,
		Long firstInvalidSequence,
		ChainViolationType violationType) {

	public static ChainVerificationResult intact(long recordsVerified) {
		return new ChainVerificationResult(true, recordsVerified, null, null, null);
	}

	public static ChainVerificationResult broken(long recordsVerified, UUID recordId, long sequence,
			ChainViolationType violationType) {
		return new ChainVerificationResult(false, recordsVerified, recordId, sequence, violationType);
	}
}
