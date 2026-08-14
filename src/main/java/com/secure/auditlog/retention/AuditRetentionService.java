package com.secure.auditlog.retention;

import java.time.Clock;
import java.time.Instant;

import com.secure.auditlog.audit.infrastructure.AuditEventJpaRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRetentionService {

	private final AuditEventJpaRepository auditEventRepository;
	private final AuditRetentionProperties retentionProperties;
	private final Clock clock;

	public AuditRetentionService(AuditEventJpaRepository auditEventRepository, AuditRetentionProperties retentionProperties,
			Clock clock) {
		this.auditEventRepository = auditEventRepository;
		this.retentionProperties = retentionProperties;
		this.clock = clock;
	}

	@Scheduled(cron = "${audit.retention.archive-cron}")
	@Transactional
	public int archiveExpiredEvents() {
		Instant archivedAt = clock.instant();
		Instant cutoff = archivedAt.minus(retentionProperties.archiveAfter());
		return auditEventRepository.archiveEventsBefore(cutoff, archivedAt);
	}
}
