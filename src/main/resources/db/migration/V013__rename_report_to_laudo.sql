-- V013: Rename report table to laudo
ALTER TABLE report RENAME TO laudo;

-- Rename indexes
ALTER INDEX idx_report_appointment RENAME TO idx_laudo_appointment;
ALTER INDEX idx_report_specialist RENAME TO idx_laudo_specialist;
ALTER INDEX idx_report_status RENAME TO idx_laudo_status;
