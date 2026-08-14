package com.secure.auditlog.audit.api;

import com.secure.auditlog.audit.application.AuditChainVerificationService;
import com.secure.auditlog.audit.domain.ChainVerificationResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditChainVerificationController {

	private final AuditChainVerificationService verificationService;

	public AuditChainVerificationController(AuditChainVerificationService verificationService) {
		this.verificationService = verificationService;
	}

	@GetMapping("/verify")
	public AuditChainVerificationResponse verify() {
		ChainVerificationResult result = verificationService.verify();
		return new AuditChainVerificationResponse(result.intact(), result.recordsVerified(), result.firstInvalidRecordId(),
				result.firstInvalidSequence(), result.violationType());
	}
}
