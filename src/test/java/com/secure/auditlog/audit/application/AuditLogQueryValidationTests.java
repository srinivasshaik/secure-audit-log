package com.secure.auditlog.audit.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.secure.auditlog.audit.domain.AuditHashingService;
import com.secure.auditlog.audit.domain.CanonicalJsonService;
import com.secure.auditlog.audit.infrastructure.AuditChainStateJpaRepository;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;

class AuditLogQueryValidationTests {

	private AuditLogService service;

	@BeforeEach
	void setUp() {
		service = new AuditLogService(mock(AuditEventJpaRepository.class), mock(AuditChainStateJpaRepository.class),
				mock(CanonicalJsonService.class), mock(AuditHashingService.class), Clock.systemUTC());
	}

	@Test
	void rejectsNegativePageNumber() {
		assertThrows(InvalidAuditEventException.class,
				() -> service.query(new AuditEventQuery(null, null, null, null, null, null, false, -1, 50)));
	}

	@Test
	void rejectsPageSizeAboveTheSafetyLimit() {
		assertThrows(InvalidAuditEventException.class,
				() -> service.query(new AuditEventQuery(null, null, null, null, null, null, false, 0, 201)));
	}

	@Test
	void rejectsReversedTimeRange() {
		assertThrows(InvalidAuditEventException.class, () -> service.query(new AuditEventQuery(null, null, null, null,
				Instant.parse("2026-08-15T12:00:01Z"), Instant.parse("2026-08-15T12:00:00Z"), false, 0, 50)));
	}
}
