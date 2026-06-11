package dev.vetra.api.modules.specialist.dto;

import java.time.Instant;
import java.util.UUID;

public record SpecialistResponse(
        UUID id,
        String userId,
        String name,
        String email,
        String phone,
        String crmv,
        String crmvState,
        String specialty,
        String baseCity,
        String baseState,
        Integer maxTravelRadiusKm,
        boolean hasOwnEquipment,
        String bio,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
