package com.secure.auditlog.audit.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.secure.auditlog.audit.domain.ChainVerificationResult;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;

/**
 * Validates Flyway migrations and the append/verification path against the production database engine.
 * H2 remains the default database for everyday development and the ordinary test suite.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlCompatibilityTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("audit_log")
			.withUsername("audit_log")
			.withPassword("test-only-password");

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
		registry.add("spring.h2.console.enabled", () -> false);
	}

	@Autowired
	private AuditLogService auditLogService;
	@Autowired
	private AuditChainVerificationService verificationService;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void appliesMigrationsAndVerifiesAnAppendedEvent() throws Exception {
		auditLogService.append(new CreateAuditEventCommand("USER_LOGIN", "postgres-test-actor", "ACCOUNT",
				"postgres-test-account", objectMapper.readTree("{\"channel\":\"testcontainers\"}")));

		ChainVerificationResult verification = verificationService.verify();
		assertTrue(verification.intact(), verification::toString);
	}
}
