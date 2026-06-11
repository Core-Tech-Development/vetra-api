package dev.vetra.api.modules.specialist.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a veterinary imaging specialist.
 * Pure Java record — no framework annotations.
 */
public record Specialist(
        UUID id,
        String userId,
        String name,
        String email,
        String phone,
        String crmv,
        String crmvState,
        Specialty specialty,
        String baseCity,
        String baseState,
        Integer maxTravelRadiusKm,
        boolean hasOwnEquipment,
        String bio,
        SpecialistStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new specialist pending approval.
     */
    public static Specialist create(String userId, String name, String email, String phone,
                                    String crmv, String crmvState, Specialty specialty,
                                    String baseCity, String baseState, Integer maxTravelRadiusKm,
                                    boolean hasOwnEquipment, String bio) {
        Instant now = Instant.now();
        return new Specialist(
                UUID.randomUUID(),
                userId,
                name,
                email,
                phone,
                crmv,
                crmvState,
                specialty,
                baseCity,
                baseState,
                maxTravelRadiusKm,
                hasOwnEquipment,
                bio,
                SpecialistStatus.PENDING_APPROVAL,
                now,
                now
        );
    }

    /**
     * Factory for restoring a specialist from persistence.
     */
    public static Specialist restore(UUID id, String userId, String name, String email, String phone,
                                     String crmv, String crmvState, Specialty specialty,
                                     String baseCity, String baseState, Integer maxTravelRadiusKm,
                                     boolean hasOwnEquipment, String bio,
                                     SpecialistStatus status, Instant createdAt, Instant updatedAt) {
        return new Specialist(id, userId, name, email, phone, crmv, crmvState, specialty,
                baseCity, baseState, maxTravelRadiusKm, hasOwnEquipment, bio, status, createdAt, updatedAt);
    }
}
