package dev.vetra.api.modules.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record AppointmentNoteResponse(
        UUID id,
        UUID appointmentId,
        String authorUserId,
        String title,
        String content,
        Instant createdAt
) {
}
