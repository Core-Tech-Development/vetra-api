-- V004 — Create patient table
-- Vetra MVP: Patient (animal) table

CREATE TABLE patient (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinic(id),
    tutor_id        UUID NOT NULL REFERENCES tutor(id),
    name            VARCHAR(255) NOT NULL,
    species         VARCHAR(50) NOT NULL,    -- e.g. 'DOG', 'CAT'
    breed           VARCHAR(100),
    sex             VARCHAR(10),             -- 'MALE', 'FEMALE'
    birth_date      DATE,
    weight_kg       DECIMAL(6,2),
    neutered        BOOLEAN,
    microchip       VARCHAR(50),
    clinical_notes  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_patient_clinic ON patient(clinic_id);
CREATE INDEX idx_patient_tutor ON patient(tutor_id);
