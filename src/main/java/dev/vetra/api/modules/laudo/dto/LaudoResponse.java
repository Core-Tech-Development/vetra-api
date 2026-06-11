package dev.vetra.api.modules.laudo.dto;

import java.time.Instant;
import java.util.UUID;

public record LaudoResponse(
        UUID id,
        UUID appointmentId,
        UUID specialistId,
        String status,
        String findings,
        String conclusion,
        String recommendations,
        String pdfStorageKey,
        Instant issuedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
