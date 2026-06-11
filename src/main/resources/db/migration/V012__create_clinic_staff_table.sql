-- V012 -- Create clinic_staff table
-- Vetra MVP: Clinic collaborators (veterinarians and secretaries)

CREATE TABLE clinic_staff (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id   UUID            NOT NULL REFERENCES clinic(id),
    user_id     VARCHAR(255),
    name        VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    phone       VARCHAR(20),
    role        VARCHAR(30)     NOT NULL,
    status      VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_clinic_staff_clinic_id ON clinic_staff(clinic_id);
CREATE INDEX idx_clinic_staff_email     ON clinic_staff(email);
CREATE INDEX idx_clinic_staff_status    ON clinic_staff(status);
