package dev.vetra.api.modules.laudo.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a diagnostic laudo issued by a specialist.
 * Pure Java record — no framework annotations.
 */
public record Laudo(
        UUID id,
        UUID appointmentId,
        UUID specialistId,
        LaudoStatus status,
        String findings,
        String conclusion,
        String recommendations,
        String pdfStorageKey,
        Instant issuedAt,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new draft laudo for an appointment.
     */
    public static Laudo createDraft(UUID appointmentId, UUID specialistId) {
        Instant now = Instant.now();
        return new Laudo(
                UUID.randomUUID(),
                appointmentId,
                specialistId,
                LaudoStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    /**
     * Factory for restoring a laudo from persistence.
     */
    public static Laudo restore(UUID id, UUID appointmentId, UUID specialistId,
                                 LaudoStatus status, String findings, String conclusion,
                                 String recommendations, String pdfStorageKey,
                                 Instant issuedAt, Instant createdAt, Instant updatedAt) {
        return new Laudo(id, appointmentId, specialistId, status, findings, conclusion,
                recommendations, pdfStorageKey, issuedAt, createdAt, updatedAt);
    }
}
