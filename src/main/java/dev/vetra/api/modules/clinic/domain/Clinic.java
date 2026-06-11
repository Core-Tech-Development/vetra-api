package dev.vetra.api.modules.clinic.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a veterinary clinic.
 * Pure Java record — no framework annotations.
 */
public record Clinic(
        UUID id,
        String name,
        String document,
        String email,
        String phone,
        String address,
        String city,
        String state,
        ClinicStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new clinic pending approval.
     */
    public static Clinic create(String name, String document, String email, String phone,
                                String address, String city, String state) {
        Instant now = Instant.now();
        return new Clinic(
                UUID.randomUUID(),
                name,
                document,
                email,
                phone,
                address,
                city,
                state,
                ClinicStatus.PENDING_APPROVAL,
                now,
                now
        );
    }

    /**
     * Factory for restoring a clinic from persistence.
     */
    public static Clinic restore(UUID id, String name, String document, String email, String phone,
                                 String address, String city, String state,
                                 ClinicStatus status, Instant createdAt, Instant updatedAt) {
        return new Clinic(id, name, document, email, phone, address, city, state, status, createdAt, updatedAt);
    }
}
