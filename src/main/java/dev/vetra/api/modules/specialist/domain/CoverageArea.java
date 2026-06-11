package dev.vetra.api.modules.specialist.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a specialist's coverage area.
 * Pure Java record — no framework annotations.
 */
public record CoverageArea(
        UUID id,
        UUID specialistId,
        String city,
        String state,
        Integer radiusKm,
        boolean active,
        Instant createdAt
) {

    /**
     * Factory for creating a new coverage area.
     */
    public static CoverageArea create(UUID specialistId, String city, String state, Integer radiusKm) {
        return new CoverageArea(
                UUID.randomUUID(),
                specialistId,
                city,
                state,
                radiusKm,
                true,
                Instant.now()
        );
    }

    /**
     * Factory for restoring a coverage area from persistence.
     */
    public static CoverageArea restore(UUID id, UUID specialistId, String city, String state,
                                       Integer radiusKm, boolean active, Instant createdAt) {
        return new CoverageArea(id, specialistId, city, state, radiusKm, active, createdAt);
    }
}
