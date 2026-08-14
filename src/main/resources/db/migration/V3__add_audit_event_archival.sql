ALTER TABLE audit_event ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_audit_event_archived_at ON audit_event (archived_at);
