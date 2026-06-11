package dev.vetra.api.modules.clinic.domain;

import java.time.Instant;
import java.util.UUID;

public record ClinicStaff(
        UUID id,
        UUID clinicId,
        String userId,
        String name,
        String email,
        String phone,
        ClinicStaffRole role,
        ClinicStaffStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClinicStaff create(UUID clinicId, String name, String email,
                                     String phone, ClinicStaffRole role) {
        Instant now = Instant.now();
        return new ClinicStaff(UUID.randomUUID(), clinicId, null, name, email,
                phone, role, ClinicStaffStatus.ACTIVE, now, now);
    }

    public static ClinicStaff restore(UUID id, UUID clinicId, String userId,
                                      String name, String email, String phone,
                                      ClinicStaffRole role, ClinicStaffStatus status,
                                      Instant createdAt, Instant updatedAt) {
        return new ClinicStaff(id, clinicId, userId, name, email, phone,
                role, status, createdAt, updatedAt);
    }

    public ClinicStaff withUserId(String userId) {
        return new ClinicStaff(id, clinicId, userId, name, email, phone,
                role, status, createdAt, updatedAt);
    }
}
