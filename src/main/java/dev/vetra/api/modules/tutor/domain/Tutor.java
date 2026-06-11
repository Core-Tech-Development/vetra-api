package dev.vetra.api.modules.tutor.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a tutor (pet owner) associated with a clinic.
 * Pure Java record -- no framework annotations.
 */
public record Tutor(
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

    /**
     * Factory for creating a new tutor.
     */
    public static Tutor create(UUID clinicId, String name, String phone, String email, String document,
                               String address, String city, String state, String zipCode) {
        Instant now = Instant.now();
        return new Tutor(UUID.randomUUID(), clinicId, name, phone, email, document,
                address, city, state, zipCode, now, now);
    }

    /**
     * Factory for restoring a tutor from persistence.
     */
    public static Tutor restore(UUID id, UUID clinicId, String name, String phone, String email,
                                String document, String address, String city, String state, String zipCode,
                                Instant createdAt, Instant updatedAt) {
        return new Tutor(id, clinicId, name, phone, email, document, address, city, state, zipCode,
                createdAt, updatedAt);
    }
}
