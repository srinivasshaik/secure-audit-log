package com.secure.auditlog.export;

import java.time.Instant;
import java.util.List;

public record AuditExportBundle(String formatVersion, Instant exportedAt, String selectorType, String selectorValue,
		long chainHeadSequence, String chainHeadHash, List<AuditExportEvent> events, String exportHash) {
}
