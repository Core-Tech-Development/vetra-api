package dev.vetra.api.modules.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID examRequestId,
        UUID specialistId,
        UUID availabilitySlotId,
        Instant scheduledStartAt,
        Instant scheduledEndAt,
        Instant actualStartAt,
        Instant actualEndAt,
        String status,
        String cancelReason,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
