-- V006 — Create availability slot table
-- Vetra MVP: Specialist availability slot table

CREATE TABLE availability_slot (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id   UUID NOT NULL REFERENCES specialist(id),
    start_at        TIMESTAMPTZ NOT NULL,
    end_at          TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',  -- 'AVAILABLE', 'RESERVED', 'BLOCKED', 'CANCELLED'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_slot_times CHECK (end_at > start_at)
);
CREATE INDEX idx_slot_specialist ON availability_slot(specialist_id);
CREATE INDEX idx_slot_status ON availability_slot(status);
CREATE INDEX idx_slot_start ON availability_slot(start_at);
