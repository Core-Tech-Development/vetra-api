package dev.vetra.api.modules.patient.dto;

import dev.vetra.api.modules.patient.domain.Patient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Static mapping between Patient domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class PatientMapper {

    private PatientMapper() {
        // utility class
    }

    public static Patient toDomain(UUID clinicId, UUID tutorId, CreatePatientRequest request) {
        LocalDate birthDate = request.birthDate() != null && !request.birthDate().isBlank()
                ? LocalDate.parse(request.birthDate())
                : null;

        return Patient.create(
                clinicId,
                tutorId,
                request.name(),
                request.species(),
                request.breed(),
                request.sex(),
                birthDate,
                request.weightKg(),
                request.neutered(),
                request.microchip(),
                request.clinicalNotes()
        );
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.id(),
                patient.clinicId(),
                patient.tutorId(),
                patient.name(),
                patient.species(),
                patient.breed(),
                patient.sex(),
                patient.birthDate(),
                patient.weightKg(),
                patient.neutered(),
                patient.microchip(),
                patient.clinicalNotes(),
                patient.createdAt(),
                patient.updatedAt()
        );
    }
}
