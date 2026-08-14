package com.secure.auditlog.redaction;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_event_redaction")
public class AuditEventRedactionEntity {

	@Id
	@Column(name = "audit_event_id")
	private UUID auditEventId;
	@Column(name = "redacted_payload", nullable = false, columnDefinition = "TEXT")
	private String redactedPayload;
	@Column(name = "redacted_paths", nullable = false, columnDefinition = "TEXT")
	private String redactedPaths;
	@Column(name = "redacted_at", nullable = false)
	private Instant redactedAt;
	@Column(name = "original_content_hash", nullable = false, length = 64)
	private String originalContentHash;
	@Column(name = "redaction_hash", nullable = false, length = 64)
	private String redactionHash;

	protected AuditEventRedactionEntity() {
	}

	public AuditEventRedactionEntity(UUID auditEventId, String redactedPayload, String redactedPaths, Instant redactedAt,
			String originalContentHash, String redactionHash) {
		this.auditEventId = auditEventId;
		this.redactedPayload = redactedPayload;
		this.redactedPaths = redactedPaths;
		this.redactedAt = redactedAt;
		this.originalContentHash = originalContentHash;
		this.redactionHash = redactionHash;
	}

	public UUID getAuditEventId() { return auditEventId; }
	public String getRedactedPayload() { return redactedPayload; }
	public String getRedactedPaths() { return redactedPaths; }
	public Instant getRedactedAt() { return redactedAt; }
	public String getOriginalContentHash() { return originalContentHash; }
	public String getRedactionHash() { return redactionHash; }
}
