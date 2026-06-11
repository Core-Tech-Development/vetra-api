package dev.vetra.api.modules.imaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ExamFileResponse(
        UUID id,
        UUID appointmentId,
        String fileName,
        String fileType,
        String contentType,
        String storageKey,
        long sizeBytes,
        String uploadedBy,
        Instant createdAt,
        String downloadUrl
) {
}
