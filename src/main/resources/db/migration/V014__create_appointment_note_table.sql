-- Appointment notes: observations and incident records during appointments
CREATE TABLE appointment_note (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointment(id),
    author_user_id  VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_appointment_note_appointment_id ON appointment_note(appointment_id);
