-- V001 — Create clinic table
-- Vetra MVP: Clinic module reference table

CREATE TABLE clinic (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL,
    document    VARCHAR(20)     NOT NULL UNIQUE,
    email       VARCHAR(255)    NOT NULL,
    phone       VARCHAR(20),
    address     TEXT,
    city        VARCHAR(100),
    state       VARCHAR(2),
    status      VARCHAR(30)     NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_clinic_document ON clinic(document);
CREATE INDEX idx_clinic_status   ON clinic(status);
