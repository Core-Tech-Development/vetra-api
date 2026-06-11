package dev.vetra.api.modules.exam.dto;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;

import java.util.UUID;

/**
 * Static mapping between ExamRequest domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class ExamRequestMapper {

    private ExamRequestMapper() {
        // utility class
    }

    public static ExamRequest toDomain(UUID clinicId, CreateExamRequestRequest request, String requestedBy) {
        ExamPriority priority = request.priority() != null && !request.priority().isBlank()
                ? ExamPriority.valueOf(request.priority())
                : ExamPriority.ROUTINE;

        return ExamRequest.create(
                clinicId,
                request.patientId(),
                request.examType(),
                priority,
                request.diagnosticHypothesis(),
                request.clinicalHistory(),
                request.additionalNotes(),
                requestedBy
        );
    }

    public static ExamRequestResponse toResponse(ExamRequest examRequest) {
        return new ExamRequestResponse(
                examRequest.id(),
                examRequest.clinicId(),
                examRequest.patientId(),
                examRequest.examType(),
                examRequest.priority().name(),
                examRequest.diagnosticHypothesis(),
                examRequest.clinicalHistory(),
                examRequest.additionalNotes(),
                examRequest.status().name(),
                examRequest.requestedBy(),
                examRequest.createdAt(),
                examRequest.updatedAt()
        );
    }
}
