package com.secure.auditlog.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import com.secure.auditlog.audit.domain.ChainVerificationResult;
import com.secure.auditlog.audit.domain.ChainViolationType;

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

	@BeforeEach
	void resetLedger() {
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

		assertTrue(verificationService.verify().intact());

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

		assertTrue(verificationService.verify().intact());
	}
}
