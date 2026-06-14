package dev.vetra.api.modules.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record SpecialistPricing(
        UUID id,
        UUID specialistId,
        String examType,
        long priceCents,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static SpecialistPricing create(UUID specialistId, String examType, long priceCents) {
        Instant now = Instant.now();
        return new SpecialistPricing(UUID.randomUUID(), specialistId, examType, priceCents, true, now, now);
    }

    public static SpecialistPricing restore(UUID id, UUID specialistId, String examType,
                                            long priceCents, boolean active,
                                            Instant createdAt, Instant updatedAt) {
        return new SpecialistPricing(id, specialistId, examType, priceCents, active, createdAt, updatedAt);
    }
}
