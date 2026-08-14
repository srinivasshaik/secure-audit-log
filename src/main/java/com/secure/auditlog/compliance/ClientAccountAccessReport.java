package com.secure.auditlog.compliance;

import java.util.List;

import com.secure.auditlog.audit.api.AuditChainVerificationResponse;
import com.secure.auditlog.audit.api.AuditEventPageResponse;

public record ClientAccountAccessReport(AuditEventPageResponse events,
		AuditChainVerificationResponse chainVerification) {
}
