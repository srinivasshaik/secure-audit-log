package com.secure.auditlog.export;

import java.time.Instant;

public record AuditExportRedaction(String redactedPaths, Instant redactedAt, String originalContentHash,
		String redactionHash) {
}
