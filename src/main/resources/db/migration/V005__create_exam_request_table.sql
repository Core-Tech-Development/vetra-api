-- V005 — Create exam request table
-- Vetra MVP: Exam request table

CREATE TABLE exam_request (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id               UUID NOT NULL REFERENCES clinic(id),
    patient_id              UUID NOT NULL REFERENCES patient(id),
    exam_type               VARCHAR(50) NOT NULL,     -- 'ABDOMINAL_ULTRASOUND', 'GESTATIONAL_ULTRASOUND', 'MUSCULOSKELETAL_ULTRASOUND'
    priority                VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',  -- 'ROUTINE', 'PRIORITY', 'URGENT'
    diagnostic_hypothesis   TEXT,
    clinical_history        TEXT,
    additional_notes        TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    requested_by            VARCHAR(255),  -- Keycloak user ID
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_exam_request_clinic ON exam_request(clinic_id);
CREATE INDEX idx_exam_request_patient ON exam_request(patient_id);
CREATE INDEX idx_exam_request_status ON exam_request(status);
