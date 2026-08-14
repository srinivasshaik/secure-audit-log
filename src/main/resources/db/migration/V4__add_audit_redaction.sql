CREATE TABLE audit_event_redaction (
    audit_event_id UUID PRIMARY KEY REFERENCES audit_event(id),
    redacted_payload TEXT NOT NULL,
    redacted_paths TEXT NOT NULL,
    redacted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    original_content_hash VARCHAR(64) NOT NULL,
    redaction_hash VARCHAR(64) NOT NULL
);
