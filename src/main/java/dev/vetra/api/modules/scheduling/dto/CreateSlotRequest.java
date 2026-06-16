package dev.vetra.api.modules.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSlotRequest(

        @NotNull(message = "Start time is required")
        Instant startAt,

        @NotNull(message = "End time is required")
        Instant endAt,

        String label
) {
}
