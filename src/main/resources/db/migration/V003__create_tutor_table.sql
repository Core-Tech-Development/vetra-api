-- V003 — Create tutor table
-- Vetra MVP: Tutor (pet owner) table

CREATE TABLE tutor (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id   UUID NOT NULL REFERENCES clinic(id),
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(255),
    document    VARCHAR(20),  -- CPF
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tutor_clinic ON tutor(clinic_id);
CREATE INDEX idx_tutor_document ON tutor(document);
