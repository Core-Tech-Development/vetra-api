package dev.vetra.api.modules.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateBulkSlotsRequest(

        @NotNull(message = "Start date is required")
        Instant startDate,

        @NotNull(message = "End date is required")
        Instant endDate,

        @NotNull(message = "Days of week are required")
        List<String> daysOfWeek,

        @NotNull(message = "Start time is required")
        String startTime,

        @NotNull(message = "End time is required")
        String endTime,

        String label,

        @NotNull(message = "Timezone is required")
        String timezone
) {
}
