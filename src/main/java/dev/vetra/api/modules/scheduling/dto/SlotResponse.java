package dev.vetra.api.modules.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID specialistId,
        Instant startAt,
        Instant endAt,
        String status,
        String label,
        UUID recurrenceGroupId,
        Instant createdAt,
        Instant updatedAt
) {
}
