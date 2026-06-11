package dev.vetra.api.modules.exam.dto;

import java.time.Instant;
import java.util.UUID;

public record ExamRequestResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        String examType,
        String priority,
        String diagnosticHypothesis,
        String clinicalHistory,
        String additionalNotes,
        String status,
        String requestedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
