package dev.vetra.api.modules.scheduling.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a specialist's availability time slot.
 * Pure Java record — no framework annotations.
 */
public record AvailabilitySlot(
        UUID id,
        UUID specialistId,
        Instant startAt,
        Instant endAt,
        SlotStatus status,
        String label,
        UUID recurrenceGroupId,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new availability slot with AVAILABLE status.
     */
    public static AvailabilitySlot create(UUID specialistId, Instant startAt, Instant endAt,
                                          String label, UUID recurrenceGroupId) {
        Instant now = Instant.now();
        return new AvailabilitySlot(
                UUID.randomUUID(),
                specialistId,
                startAt,
                endAt,
                SlotStatus.AVAILABLE,
                label,
                recurrenceGroupId,
                now,
                now
        );
    }

    /**
     * Convenience factory without label/recurrence fields.
     */
    public static AvailabilitySlot create(UUID specialistId, Instant startAt, Instant endAt) {
        return create(specialistId, startAt, endAt, null, null);
    }

    /**
     * Factory for restoring an availability slot from persistence.
     */
    public static AvailabilitySlot restore(UUID id, UUID specialistId, Instant startAt, Instant endAt,
                                           SlotStatus status, String label, UUID recurrenceGroupId,
                                           Instant createdAt, Instant updatedAt) {
        return new AvailabilitySlot(id, specialistId, startAt, endAt, status, label, recurrenceGroupId,
                createdAt, updatedAt);
    }
}
