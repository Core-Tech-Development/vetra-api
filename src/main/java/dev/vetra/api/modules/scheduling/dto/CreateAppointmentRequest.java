package dev.vetra.api.modules.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(

        @NotNull(message = "Exam request ID is required")
        UUID examRequestId,

        @NotNull(message = "Specialist ID is required")
        UUID specialistId,

        UUID availabilitySlotId,

        Instant scheduledStartAt,

        Instant scheduledEndAt
) {
}
