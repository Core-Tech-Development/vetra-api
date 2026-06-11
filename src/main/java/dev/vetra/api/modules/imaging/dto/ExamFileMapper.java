package dev.vetra.api.modules.imaging.dto;

import dev.vetra.api.modules.imaging.domain.ExamFile;

/**
 * Static mapping between ExamFile domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class ExamFileMapper {

    private ExamFileMapper() {
        // utility class
    }

    public static ExamFileResponse toResponse(ExamFile file) {
        return new ExamFileResponse(
                file.id(),
                file.appointmentId(),
                file.fileName(),
                file.fileType(),
                file.contentType(),
                file.storageKey(),
                file.sizeBytes(),
                file.uploadedBy(),
                file.createdAt(),
                null
        );
    }

    public static ExamFileResponse toResponseWithUrl(ExamFile file, String downloadUrl) {
        return new ExamFileResponse(
                file.id(),
                file.appointmentId(),
                file.fileName(),
                file.fileType(),
                file.contentType(),
                file.storageKey(),
                file.sizeBytes(),
                file.uploadedBy(),
                file.createdAt(),
                downloadUrl
        );
    }
}
