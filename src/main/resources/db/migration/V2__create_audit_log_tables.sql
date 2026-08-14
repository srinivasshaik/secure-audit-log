CREATE TABLE audit_chain_state (
    id BIGINT PRIMARY KEY,
    last_sequence BIGINT NOT NULL,
    last_hash VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL
);

INSERT INTO audit_chain_state (id, last_sequence, last_hash, version)
VALUES (1, 0, '0000000000000000000000000000000000000000000000000000000000000000', 0);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    chain_sequence BIGINT NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ingested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    content_hash VARCHAR(64) NOT NULL
);

CREATE INDEX idx_audit_event_actor_time ON audit_event (actor_id, occurred_at);
CREATE INDEX idx_audit_event_resource_time ON audit_event (resource_type, resource_id, occurred_at);
CREATE INDEX idx_audit_event_type_time ON audit_event (event_type, occurred_at);
