CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE availability_slot
    ADD COLUMN label VARCHAR(100),
    ADD COLUMN recurrence_group_id UUID;

ALTER TABLE availability_slot
    ADD CONSTRAINT no_overlapping_slots
    EXCLUDE USING gist (
        specialist_id WITH =,
        tstzrange(start_at, end_at) WITH &&
    ) WHERE (status NOT IN ('CANCELLED'));

CREATE INDEX idx_slot_specialist_range ON availability_slot(specialist_id, start_at, end_at);
