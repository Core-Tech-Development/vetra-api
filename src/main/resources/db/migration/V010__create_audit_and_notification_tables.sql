-- V010 — Create audit and notification tables
-- Vetra MVP: Audit log and notification tables

CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   VARCHAR(255),
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,    -- 'CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE'
    previous_value  JSONB,
    new_value       JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);

CREATE TABLE notification (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   VARCHAR(255) NOT NULL,
    channel             VARCHAR(20) NOT NULL DEFAULT 'EMAIL',  -- 'EMAIL', 'IN_APP'
    type                VARCHAR(50) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- 'PENDING', 'SENT', 'FAILED'
    subject             VARCHAR(500),
    payload             JSONB,
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_recipient ON notification(recipient_user_id);
CREATE INDEX idx_notification_status ON notification(status);
