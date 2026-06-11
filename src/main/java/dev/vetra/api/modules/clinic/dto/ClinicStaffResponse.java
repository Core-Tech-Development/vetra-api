package dev.vetra.api.modules.clinic.dto;

import java.time.Instant;
import java.util.UUID;

public record ClinicStaffResponse(
        UUID id,
        UUID clinicId,
        String userId,
        String name,
        String email,
        String phone,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
