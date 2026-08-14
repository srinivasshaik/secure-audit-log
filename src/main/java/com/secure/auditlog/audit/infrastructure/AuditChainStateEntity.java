package com.secure.auditlog.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainStateEntity {

	@Id
	private long id;
	@Column(name = "last_sequence", nullable = false)
	private long lastSequence;
	@Column(name = "last_hash", nullable = false, length = 64)
	private String lastHash;
	@Version
	private long version;

	protected AuditChainStateEntity() {
	}

	public long nextSequence() {
		lastSequence++;
		return lastSequence;
	}

	public String getLastHash() { return lastHash; }
	public void setLastHash(String lastHash) { this.lastHash = lastHash; }
}
