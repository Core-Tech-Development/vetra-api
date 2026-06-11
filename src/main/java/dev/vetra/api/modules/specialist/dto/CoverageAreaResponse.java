package dev.vetra.api.modules.specialist.dto;

import java.time.Instant;
import java.util.UUID;

public record CoverageAreaResponse(
        UUID id,
        UUID specialistId,
        String city,
        String state,
        Integer radiusKm,
        boolean active,
        Instant createdAt
) {
}
