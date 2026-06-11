package dev.vetra.api.modules.patient.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
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
}
