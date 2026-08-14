package com.secure.auditlog.audit.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

	@Id
	private UUID id;
	@Column(name = "chain_sequence", nullable = false, unique = true)
	private long chainSequence;
	@Column(name = "event_type", nullable = false)
	private String eventType;
	@Column(name = "actor_id", nullable = false)
	private String actorId;
	@Column(name = "resource_type", nullable = false)
	private String resourceType;
	@Column(name = "resource_id", nullable = false)
	private String resourceId;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;
	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;
	@Column(name = "ingested_at", nullable = false)
	private Instant ingestedAt;
	@Column(name = "previous_hash", nullable = false, length = 64)
	private String previousHash;
	@Column(name = "content_hash", nullable = false, length = 64)
	private String contentHash;
	@Column(name = "archived_at")
	private Instant archivedAt;

	protected AuditEventEntity() {
	}

	public AuditEventEntity(UUID id, long chainSequence, String eventType, String actorId, String resourceType,
			String resourceId, String payload, Instant occurredAt, Instant ingestedAt, String previousHash,
			String contentHash) {
		this.id = id;
		this.chainSequence = chainSequence;
		this.eventType = eventType;
		this.actorId = actorId;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.payload = payload;
		this.occurredAt = occurredAt;
		this.ingestedAt = ingestedAt;
		this.previousHash = previousHash;
		this.contentHash = contentHash;
	}

	public UUID getId() { return id; }
	public long getChainSequence() { return chainSequence; }
	public String getEventType() { return eventType; }
	public String getActorId() { return actorId; }
	public String getResourceType() { return resourceType; }
	public String getResourceId() { return resourceId; }
	public String getPayload() { return payload; }
	public Instant getOccurredAt() { return occurredAt; }
	public Instant getIngestedAt() { return ingestedAt; }
	public String getPreviousHash() { return previousHash; }
	public String getContentHash() { return contentHash; }
	public Instant getArchivedAt() { return archivedAt; }
	public void replacePayload(String payload) { this.payload = payload; }
}
