-- V016 -- Create billing module tables

-- Exam type pricing: platform-wide default prices per exam type
CREATE TABLE exam_type_pricing (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_type             VARCHAR(100) NOT NULL UNIQUE,
    price_cents           BIGINT NOT NULL,
    platform_fee_percent  NUMERIC(5,2) NOT NULL DEFAULT 12.00,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_exam_type_pricing_exam_type ON exam_type_pricing(exam_type);

-- Specialist custom pricing (optional override per specialist + exam type)
CREATE TABLE specialist_pricing (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    specialist_id   UUID NOT NULL REFERENCES specialist(id),
    exam_type       VARCHAR(100) NOT NULL,
    price_cents     BIGINT NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (specialist_id, exam_type)
);

CREATE INDEX idx_specialist_pricing_specialist ON specialist_pricing(specialist_id);

-- Asaas customer mapping (clinic -> Asaas customer ID)
CREATE TABLE asaas_customer (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id         UUID NOT NULL UNIQUE REFERENCES clinic(id),
    asaas_customer_id VARCHAR(255) NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_asaas_customer_clinic ON asaas_customer(clinic_id);

-- Billing record: one per issued laudo
CREATE TABLE billing_record (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    laudo_id               UUID NOT NULL UNIQUE REFERENCES laudo(id),
    appointment_id         UUID NOT NULL REFERENCES appointment(id),
    clinic_id              UUID NOT NULL REFERENCES clinic(id),
    specialist_id          UUID NOT NULL REFERENCES specialist(id),
    exam_type              VARCHAR(100) NOT NULL,
    total_cents            BIGINT NOT NULL,
    platform_fee_cents     BIGINT NOT NULL,
    specialist_share_cents BIGINT NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT_CREATION',
    asaas_payment_id       VARCHAR(255),
    error_message          TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_billing_record_clinic ON billing_record(clinic_id);
CREATE INDEX idx_billing_record_specialist ON billing_record(specialist_id);
CREATE INDEX idx_billing_record_status ON billing_record(status);
CREATE INDEX idx_billing_record_laudo ON billing_record(laudo_id);

-- Billing payment: tracks Asaas payment details (PIX QR, boleto URL, etc.)
CREATE TABLE billing_payment (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    billing_record_id UUID NOT NULL REFERENCES billing_record(id),
    asaas_payment_id  VARCHAR(255) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    billing_type      VARCHAR(30),
    pix_qr_code       TEXT,
    pix_copy_paste    TEXT,
    boleto_url        TEXT,
    invoice_url       TEXT,
    due_date          DATE,
    paid_at           TIMESTAMPTZ,
    value_cents       BIGINT NOT NULL,
    net_value_cents   BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_billing_payment_record ON billing_payment(billing_record_id);
CREATE INDEX idx_billing_payment_asaas ON billing_payment(asaas_payment_id);

-- Webhook event log: audit trail for all incoming Asaas webhooks
CREATE TABLE webhook_event_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id         VARCHAR(255),
    event_type       VARCHAR(100) NOT NULL,
    asaas_payment_id VARCHAR(255),
    payload          JSONB NOT NULL,
    processed        BOOLEAN NOT NULL DEFAULT false,
    error_message    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_event_log_event_id ON webhook_event_log(event_id);
CREATE INDEX idx_webhook_event_log_asaas_payment ON webhook_event_log(asaas_payment_id);
CREATE INDEX idx_webhook_event_log_processed ON webhook_event_log(processed);
