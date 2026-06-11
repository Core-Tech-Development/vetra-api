package dev.vetra.api.modules.exam.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing an exam request created by a clinic for a patient.
 * Pure Java record -- no framework annotations.
 */
public record ExamRequest(
        UUID id,
        UUID clinicId,
        UUID patientId,
        String examType,
        ExamPriority priority,
        String diagnosticHypothesis,
        String clinicalHistory,
        String additionalNotes,
        ExamRequestStatus status,
        String requestedBy,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new exam request with CREATED status.
     */
    public static ExamRequest create(UUID clinicId, UUID patientId, String examType, ExamPriority priority,
                                     String diagnosticHypothesis, String clinicalHistory,
                                     String additionalNotes, String requestedBy) {
        Instant now = Instant.now();
        return new ExamRequest(
                UUID.randomUUID(),
                clinicId,
                patientId,
                examType,
                priority,
                diagnosticHypothesis,
                clinicalHistory,
                additionalNotes,
                ExamRequestStatus.CREATED,
                requestedBy,
                now,
                now
        );
    }

    /**
     * Factory for restoring an exam request from persistence.
     */
    public static ExamRequest restore(UUID id, UUID clinicId, UUID patientId, String examType,
                                      ExamPriority priority, String diagnosticHypothesis,
                                      String clinicalHistory, String additionalNotes,
                                      ExamRequestStatus status, String requestedBy,
                                      Instant createdAt, Instant updatedAt) {
        return new ExamRequest(id, clinicId, patientId, examType, priority,
                diagnosticHypothesis, clinicalHistory, additionalNotes,
                status, requestedBy, createdAt, updatedAt);
    }
}
