ALTER TABLE notification
    ADD COLUMN read_at TIMESTAMPTZ,
    ADD COLUMN reference_id UUID,
    ADD COLUMN reference_type VARCHAR(50);

CREATE INDEX idx_notification_unread ON notification(recipient_user_id) WHERE read_at IS NULL;
CREATE INDEX idx_notification_reference ON notification(reference_type, reference_id);
