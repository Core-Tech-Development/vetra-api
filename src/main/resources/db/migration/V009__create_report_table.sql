-- V009 — Create report table
-- Vetra MVP: Diagnostic report table

CREATE TABLE report (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointment(id),
    specialist_id   UUID NOT NULL REFERENCES specialist(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- 'DRAFT', 'ISSUED', 'REVISED', 'CANCELLED'
    findings        TEXT,
    conclusion      TEXT,
    recommendations TEXT,
    pdf_storage_key VARCHAR(1000),
    issued_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_report_appointment ON report(appointment_id);
CREATE INDEX idx_report_specialist ON report(specialist_id);
CREATE INDEX idx_report_status ON report(status);
