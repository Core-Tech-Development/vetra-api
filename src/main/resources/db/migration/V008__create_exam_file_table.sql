-- V008 — Create exam file table
-- Vetra MVP: Exam file metadata table (files stored in MinIO/S3)

CREATE TABLE exam_file (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointment(id),
    file_name       VARCHAR(500) NOT NULL,
    file_type       VARCHAR(50) NOT NULL,   -- 'IMAGE', 'VIDEO', 'PDF'
    content_type    VARCHAR(100) NOT NULL,   -- MIME type
    storage_key     VARCHAR(1000) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    uploaded_by     VARCHAR(255) NOT NULL,   -- Keycloak user ID
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_exam_file_appointment ON exam_file(appointment_id);
