package com.secure.auditlog.redaction;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRedactionRepository extends JpaRepository<AuditEventRedactionEntity, UUID> {

	List<AuditEventRedactionEntity> findAllByAuditEventIdIn(Collection<UUID> auditEventIds);
}
