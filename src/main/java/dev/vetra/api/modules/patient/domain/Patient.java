package dev.vetra.api.modules.patient.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain model representing a patient (animal) associated with a tutor and clinic.
 * Pure Java record -- no framework annotations.
 */
public record Patient(
        UUID id,
        UUID clinicId,
        UUID tutorId,
        String name,
        String species,
        String breed,
        String sex,
        LocalDate birthDate,
        BigDecimal weightKg,
        Boolean neutered,
        String microchip,
        String clinicalNotes,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new patient.
     */
    public static Patient create(UUID clinicId, UUID tutorId, String name, String species,
                                 String breed, String sex, LocalDate birthDate, BigDecimal weightKg,
                                 Boolean neutered, String microchip, String clinicalNotes) {
        Instant now = Instant.now();
        return new Patient(
                UUID.randomUUID(),
                clinicId,
                tutorId,
                name,
                species,
                breed,
                sex,
                birthDate,
                weightKg,
                neutered,
                microchip,
                clinicalNotes,
                now,
                now
        );
    }

    /**
     * Factory for restoring a patient from persistence.
     */
    public static Patient restore(UUID id, UUID clinicId, UUID tutorId, String name, String species,
                                  String breed, String sex, LocalDate birthDate, BigDecimal weightKg,
                                  Boolean neutered, String microchip, String clinicalNotes,
                                  Instant createdAt, Instant updatedAt) {
        return new Patient(id, clinicId, tutorId, name, species, breed, sex, birthDate,
                weightKg, neutered, microchip, clinicalNotes, createdAt, updatedAt);
    }
}
