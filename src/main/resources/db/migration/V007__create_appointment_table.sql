-- V007 — Create appointment table
-- Vetra MVP: Appointment table

CREATE TABLE appointment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_request_id     UUID NOT NULL REFERENCES exam_request(id),
    specialist_id       UUID NOT NULL REFERENCES specialist(id),
    availability_slot_id UUID REFERENCES availability_slot(id),
    scheduled_start_at  TIMESTAMPTZ,
    scheduled_end_at    TIMESTAMPTZ,
    actual_start_at     TIMESTAMPTZ,
    actual_end_at       TIMESTAMPTZ,
    status              VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    cancel_reason       TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Status: CREATED, WAITING_SPECIALIST_ACCEPTANCE, ACCEPTED, SCHEDULED, IN_TRANSIT, IN_SERVICE, EXAM_DONE, WAITING_REPORT, REPORT_ISSUED, COMPLETED, CANCELLED, NO_SHOW
CREATE INDEX idx_appointment_exam_request ON appointment(exam_request_id);
CREATE INDEX idx_appointment_specialist ON appointment(specialist_id);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_scheduled ON appointment(scheduled_start_at);
