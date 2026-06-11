package dev.vetra.api.modules.tutor.dto;

import java.time.Instant;
import java.util.UUID;

public record TutorResponse(
        UUID id,
        UUID clinicId,
        String name,
        String phone,
        String email,
        String document,
        String address,
        String city,
        String state,
        String zipCode,
        Instant createdAt,
        Instant updatedAt
) {
}
