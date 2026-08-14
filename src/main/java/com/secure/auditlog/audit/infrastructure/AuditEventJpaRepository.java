package com.secure.auditlog.audit.infrastructure;

import java.util.UUID;
import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {

	List<AuditEventEntity> findAllByOrderByChainSequenceAsc();

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update AuditEventEntity event set event.archivedAt = :archivedAt "
			+ "where event.archivedAt is null and event.occurredAt < :cutoff")
	int archiveEventsBefore(@Param("cutoff") Instant cutoff, @Param("archivedAt") Instant archivedAt);
}
