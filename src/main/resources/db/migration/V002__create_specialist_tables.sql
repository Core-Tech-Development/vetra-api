-- V002 — Create specialist tables
-- Vetra MVP: Specialist and coverage area tables

CREATE TABLE specialist (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               VARCHAR(255) NOT NULL UNIQUE,  -- Keycloak subject ID
    name                  VARCHAR(255) NOT NULL,
    email                 VARCHAR(255) NOT NULL,
    phone                 VARCHAR(20),
    crmv                  VARCHAR(20) NOT NULL,
    crmv_state            VARCHAR(2) NOT NULL,
    specialty             VARCHAR(100) NOT NULL,  -- e.g. 'ULTRASOUND'
    base_city             VARCHAR(100),
    base_state            VARCHAR(2),
    max_travel_radius_km  INTEGER,
    has_own_equipment     BOOLEAN NOT NULL DEFAULT false,
    bio                   TEXT,
    status                VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_specialist_user_id ON specialist(user_id);
CREATE INDEX idx_specialist_status ON specialist(status);
CREATE INDEX idx_specialist_specialty ON specialist(specialty);

CREATE TABLE specialist_coverage_area (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id   UUID NOT NULL REFERENCES specialist(id),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(2) NOT NULL,
    radius_km       INTEGER,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_coverage_specialist ON specialist_coverage_area(specialist_id);
CREATE INDEX idx_coverage_city_state ON specialist_coverage_area(city, state);
