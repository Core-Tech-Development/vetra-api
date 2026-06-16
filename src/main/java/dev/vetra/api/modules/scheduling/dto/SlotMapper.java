package dev.vetra.api.modules.scheduling.dto;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;

import java.util.UUID;

/**
 * Static mapping between AvailabilitySlot domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class SlotMapper {

    private SlotMapper() {
        // utility class
    }

    public static AvailabilitySlot toDomain(UUID specialistId, CreateSlotRequest request) {
        return AvailabilitySlot.create(
                specialistId,
                request.startAt(),
                request.endAt(),
                request.label(),
                null
        );
    }

    public static SlotResponse toResponse(AvailabilitySlot slot) {
        return new SlotResponse(
                slot.id(),
                slot.specialistId(),
                slot.startAt(),
                slot.endAt(),
                slot.status().name(),
                slot.label(),
                slot.recurrenceGroupId(),
                slot.createdAt(),
                slot.updatedAt()
        );
    }
}
