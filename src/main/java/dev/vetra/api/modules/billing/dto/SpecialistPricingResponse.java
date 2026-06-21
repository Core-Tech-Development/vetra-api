package dev.vetra.api.modules.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record SpecialistPricingResponse(
        UUID id,
        UUID specialistId,
        String examType,
        long priceCents,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
