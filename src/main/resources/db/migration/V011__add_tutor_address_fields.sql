-- V011 — Add address fields to tutor table
ALTER TABLE tutor ADD COLUMN address VARCHAR(500);
ALTER TABLE tutor ADD COLUMN city VARCHAR(100);
ALTER TABLE tutor ADD COLUMN state VARCHAR(2);
ALTER TABLE tutor ADD COLUMN zip_code VARCHAR(10);
