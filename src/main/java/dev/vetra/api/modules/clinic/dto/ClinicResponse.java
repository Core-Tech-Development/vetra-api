package dev.vetra.api.modules.clinic.dto;

import java.time.Instant;
import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        String document,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
