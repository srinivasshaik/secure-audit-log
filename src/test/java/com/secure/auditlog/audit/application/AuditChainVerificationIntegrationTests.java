package com.secure.auditlog.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import com.secure.auditlog.audit.domain.ChainVerificationResult;
import com.secure.auditlog.audit.domain.ChainViolationType;
import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;
import com.secure.auditlog.compliance.ComplianceReportingService;
import com.secure.auditlog.export.AuditExportService;
import com.secure.auditlog.redaction.AuditRedactionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class AuditChainVerificationIntegrationTests {

	@Autowired
	private AuditLogService auditLogService;
	@Autowired
	private AuditChainVerificationService verificationService;
	@Autowired
	private JdbcClient jdbcClient;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private AuditRedactionService redactionService;
	@Autowired
	private AuditEventJpaRepository auditEventRepository;
	@Autowired
	private AuditExportService exportService;
	@Autowired
	private ComplianceReportingService complianceReportingService;

	@BeforeEach
	void resetLedger() {
		jdbcClient.sql("DELETE FROM audit_event_redaction").update();
		jdbcClient.sql("DELETE FROM audit_event").update();
		jdbcClient.sql("UPDATE audit_chain_state SET last_sequence = 0, "
				+ "last_hash = '0000000000000000000000000000000000000000000000000000000000000000', version = 0 WHERE id = 1")
				.update();
	}

	@Test
	void detectsDirectDatastoreModification() throws Exception {
		var firstEvent = auditLogService.append(new CreateAuditEventCommand("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"channel\":\"web\"}")));
		auditLogService.append(new CreateAuditEventCommand("RECORD_UPDATED", "actor-2", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"field\":\"status\"}")));

		ChainVerificationResult initialVerification = verificationService.verify();
		assertTrue(initialVerification.intact(), initialVerification::toString);

		jdbcClient.sql("UPDATE audit_event SET actor_id = :actorId WHERE id = :id")
				.param("actorId", "tampered-actor")
				.param("id", firstEvent.getId())
				.update();

		ChainVerificationResult result = verificationService.verify();
		assertFalse(result.intact());
		assertEquals(firstEvent.getId(), result.firstInvalidRecordId());
		assertEquals(ChainViolationType.CONTENT_HASH_MISMATCH, result.violationType());
	}

	@Test
	void acceptsLegitimatelyArchivedRecordsDuringVerification() throws Exception {
		var event = auditLogService.append(new CreateAuditEventCommand("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"channel\":\"web\"}")));

		jdbcClient.sql("UPDATE audit_event SET archived_at = :archivedAt WHERE id = :id")
				.param("archivedAt", Instant.parse("2027-01-01T00:00:00Z"))
				.param("id", event.getId())
				.update();

		ChainVerificationResult verification = verificationService.verify();
		assertTrue(verification.intact(), verification::toString);
	}

	@Test
	void redactsSensitivePayloadAndPreservesVerifiableEvidence() throws Exception {
		var event = auditLogService.append(new CreateAuditEventCommand("ACCOUNT_ACCESSED", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"accountNumber\":\"123456789\",\"channel\":\"web\"}")));

		redactionService.redact(event.getId(), List.of("/accountNumber"));

		assertFalse(auditEventRepository.findById(event.getId()).orElseThrow().getPayload().contains("123456789"));
		ChainVerificationResult verification = verificationService.verify();
		assertTrue(verification.intact(), verification::toString);
	}

	@Test
	void createsAStableVerifiableExportBundle() throws Exception {
		auditLogService.append(new CreateAuditEventCommand("USER_LOGIN", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"channel\":\"web\"}")));

		var bundle = exportService.export("actor-1", null);

		assertEquals("audit-export-v1", bundle.formatVersion());
		assertEquals(1, bundle.events().size());
		assertEquals(64, bundle.exportHash().length());
	}

	@Test
	void reportsOnlyClientAccountAccessEventsWithChainStatus() throws Exception {
		auditLogService.append(new CreateAuditEventCommand("ACCOUNT_ACCESSED", "examiner-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"channel\":\"web\"}")));
		auditLogService.append(new CreateAuditEventCommand("RECORD_UPDATED", "examiner-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"field\":\"status\"}")));

		var report = complianceReportingService.report("examiner-1", "account-1", Instant.parse("2020-01-01T00:00:00Z"),
				Instant.parse("2030-01-01T00:00:00Z"), 0, 50);

		assertEquals(1, report.events().totalElements());
		assertEquals("ACCOUNT_ACCESSED", report.events().content().getFirst().eventType());
		assertTrue(report.chainVerification().intact());
	}
}
