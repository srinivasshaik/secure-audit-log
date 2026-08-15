package com.secure.auditlog.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
	void rejectsNonObjectAuditPayloads() throws Exception {
		assertThrows(InvalidAuditEventException.class, () -> auditLogService.append(new CreateAuditEventCommand(
				"INVALID", "actor-1", "ACCOUNT", "account-1", objectMapper.readTree("[]"))));
	}

	@Test
	void appliesEveryQueryFilterAndArchivedVisibilityRule() throws Exception {
		var matching = auditLogService.append(new CreateAuditEventCommand("ACCOUNT_ACCESSED", "actor-1", "ACCOUNT",
				"account-1", objectMapper.readTree("{\"channel\":\"web\"}")));
		auditLogService.append(new CreateAuditEventCommand("OTHER", "actor-2", "CASE", "case-1",
				objectMapper.readTree("{\"channel\":\"batch\"}")));

		var result = auditLogService.query(new AuditEventQuery("actor-1", "ACCOUNT", "account-1", "ACCOUNT_ACCESSED",
				matching.getOccurredAt().minusSeconds(1), matching.getOccurredAt().plusSeconds(1), false, 0, 10));
		assertEquals(List.of(matching.getId()), result.map(event -> event.getId()).getContent());

		jdbcClient.sql("UPDATE audit_event SET archived_at = :at WHERE id = :id")
				.param("at", Instant.parse("2027-01-01T00:00:00Z")).param("id", matching.getId()).update();
		assertTrue(auditLogService.query(new AuditEventQuery("actor-1", null, null, null, null, null, false, 0, 10)).isEmpty());
		assertEquals(1, auditLogService.query(
				new AuditEventQuery("actor-1", null, null, null, null, null, true, 0, 10)).getTotalElements());
	}

	@Test
	void rejectsInvalidRedactionPathSets() {
		UUID id = UUID.randomUUID();
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(id, null));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(id, List.of()));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(id, List.of("/secret", "/secret")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(id, List.of("/")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(id, List.of("secret")));
	}

	@Test
	void rejectsMissingAndRepeatedRedactions() throws Exception {
		assertThrows(InvalidAuditEventException.class,
				() -> redactionService.redact(UUID.randomUUID(), List.of("/secret")));
		var event = auditLogService.append(new CreateAuditEventCommand("SECRET", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"secret\":\"value\"}")));
		redactionService.redact(event.getId(), List.of("/secret"));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(event.getId(), List.of("/secret")));
	}

	@Test
	void redactsNestedArraysAndEscapedObjectKeys() throws Exception {
		var event = auditLogService.append(new CreateAuditEventCommand("SECRET", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"items\":[{\"secret\":\"value\"}],\"a/b\":{\"~key\":42}}")));
		redactionService.redact(event.getId(), List.of("/items/0/secret", "/a~1b/~0key"));
		String payload = auditEventRepository.findById(event.getId()).orElseThrow().getPayload();
		assertEquals("{\"a/b\":{\"~key\":\"[REDACTED]\"},\"items\":[{\"secret\":\"[REDACTED]\"}]}", payload);
		assertTrue(verificationService.verify().intact());
	}

	@Test
	void rejectsPointersToContainersMissingValuesAndInvalidArrayIndexes() throws Exception {
		var event = auditLogService.append(new CreateAuditEventCommand("SECRET", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"items\":[{\"secret\":\"value\"}],\"nested\":{\"value\":1}}")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(event.getId(), List.of("/nested")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(event.getId(), List.of("/missing/value")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(event.getId(), List.of("/items/not-a-number")));
		assertThrows(InvalidAuditEventException.class, () -> redactionService.redact(event.getId(), List.of("/items/9")));
	}

	@Test
	void detectsPreviousHashAndChainStateCorruption() throws Exception {
		var event = auditLogService.append(new CreateAuditEventCommand("EVENT", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"value\":1}")));
		jdbcClient.sql("UPDATE audit_event SET previous_hash = :hash WHERE id = :id")
				.param("hash", "f".repeat(64)).param("id", event.getId()).update();
		assertEquals(ChainViolationType.PREVIOUS_HASH_MISMATCH, verificationService.verify().violationType());

		jdbcClient.sql("UPDATE audit_event SET previous_hash = :hash WHERE id = :id")
				.param("hash", "0".repeat(64)).param("id", event.getId()).update();
		jdbcClient.sql("UPDATE audit_chain_state SET last_hash = :hash WHERE id = 1").param("hash", "f".repeat(64)).update();
		assertEquals(ChainViolationType.CHAIN_STATE_HASH_MISMATCH, verificationService.verify().violationType());

		jdbcClient.sql("UPDATE audit_chain_state SET last_sequence = 99 WHERE id = 1").update();
		assertEquals(ChainViolationType.CHAIN_STATE_SEQUENCE_MISMATCH, verificationService.verify().violationType());
	}

	@Test
	void serializesConcurrentAppendsWithoutBreakingTheChain() throws Exception {
		int appendCount = 12;
		try (var executor = Executors.newFixedThreadPool(6)) {
			var tasks = IntStream.range(0, appendCount)
					.mapToObj(index -> (java.util.concurrent.Callable<Long>) () -> auditLogService.append(
							new CreateAuditEventCommand("CONCURRENT_EVENT", "actor-" + index, "ACCOUNT",
									"account-" + index, objectMapper.readTree("{\"index\":" + index + "}")))
							.getChainSequence())
					.toList();
			var futures = executor.invokeAll(tasks);
			executor.shutdown();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
			var sequences = futures.stream().map(future -> {
				try { return future.get(); } catch (Exception exception) { throw new AssertionError(exception); }
			}).sorted().toList();
			assertEquals(IntStream.rangeClosed(1, appendCount).asLongStream().boxed().toList(), sequences);
		}
		assertTrue(verificationService.verify().intact());
	}

	@Test
	void detectsADeletedLedgerRecordAsASequenceGap() throws Exception {
		var first = auditLogService.append(new CreateAuditEventCommand("FIRST", "actor-1", "ACCOUNT", "account-1",
				objectMapper.readTree("{\"position\":1}")));
		auditLogService.append(new CreateAuditEventCommand("SECOND", "actor-2", "ACCOUNT", "account-2",
				objectMapper.readTree("{\"position\":2}")));

		jdbcClient.sql("DELETE FROM audit_event WHERE id = :id").param("id", first.getId()).update();

		ChainVerificationResult result = verificationService.verify();
		assertFalse(result.intact());
		assertEquals(ChainViolationType.SEQUENCE_GAP, result.violationType());
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
