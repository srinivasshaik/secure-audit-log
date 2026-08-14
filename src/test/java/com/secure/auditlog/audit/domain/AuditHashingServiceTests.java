package com.secure.auditlog.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AuditHashingServiceTests {

	private final AuditHashingService hashingService = new AuditHashingService();

	@Test
	void producesTheSameHashForTheSameCanonicalContent() {
		AuditEventContent content = new AuditEventContent("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				"{\"channel\":\"web\"}", Instant.parse("2026-08-14T12:00:00Z"));

		assertEquals(hashingService.contentHash(content), hashingService.contentHash(content));
	}

	@Test
	void producesADifferentHashWhenAnyAuditedFieldChanges() {
		AuditEventContent original = new AuditEventContent("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				"{\"channel\":\"web\"}", Instant.parse("2026-08-14T12:00:00Z"));
		AuditEventContent changed = new AuditEventContent("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				"{\"channel\":\"mobile\"}", Instant.parse("2026-08-14T12:00:00Z"));

		assertNotEquals(hashingService.contentHash(original), hashingService.contentHash(changed));
	}
}
