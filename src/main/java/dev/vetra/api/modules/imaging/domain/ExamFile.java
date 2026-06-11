package dev.vetra.api.modules.imaging.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a file (image, video, PDF) attached to an appointment exam.
 * Actual file content is stored in MinIO/S3; this record holds metadata.
 * Pure Java record — no framework annotations.
 */
public record ExamFile(
        UUID id,
        UUID appointmentId,
        String fileName,
        String fileType,
        String contentType,
        String storageKey,
        long sizeBytes,
        String uploadedBy,
        Instant createdAt
) {

    /**
     * Factory for creating a new exam file metadata entry.
     */
    public static ExamFile create(UUID appointmentId, String fileName, String fileType,
                                  String contentType, String storageKey, long sizeBytes,
                                  String uploadedBy) {
        return new ExamFile(
                UUID.randomUUID(),
                appointmentId,
                fileName,
                fileType,
                contentType,
                storageKey,
                sizeBytes,
                uploadedBy,
                Instant.now()
        );
    }

    /**
     * Factory for restoring an exam file from persistence.
     */
    public static ExamFile restore(UUID id, UUID appointmentId, String fileName, String fileType,
                                   String contentType, String storageKey, long sizeBytes,
                                   String uploadedBy, Instant createdAt) {
        return new ExamFile(id, appointmentId, fileName, fileType, contentType, storageKey,
                sizeBytes, uploadedBy, createdAt);
    }
}
