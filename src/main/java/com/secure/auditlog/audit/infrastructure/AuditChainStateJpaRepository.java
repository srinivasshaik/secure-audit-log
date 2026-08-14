package com.secure.auditlog.audit.infrastructure;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AuditChainStateJpaRepository extends JpaRepository<AuditChainStateEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select state from AuditChainStateEntity state where state.id = 1")
	Optional<AuditChainStateEntity> findSingletonForUpdate();
}
