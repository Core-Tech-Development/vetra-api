-- Add title column to appointment_note
ALTER TABLE appointment_note ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT '';
