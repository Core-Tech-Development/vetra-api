package dev.vetra.api.modules.clinic.dto;

import dev.vetra.api.modules.clinic.domain.Clinic;

/**
 * Static mapping between Clinic domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class ClinicMapper {

    private ClinicMapper() {
        // utility class
    }

    public static Clinic toDomain(CreateClinicRequest request) {
        return Clinic.create(
                request.name(),
                request.document(),
                request.email(),
                request.phone(),
                request.address(),
                request.city(),
                request.state()
        );
    }

    public static ClinicResponse toResponse(Clinic clinic) {
        return new ClinicResponse(
                clinic.id(),
                clinic.name(),
                clinic.document(),
                clinic.email(),
                clinic.phone(),
                clinic.address(),
                clinic.city(),
                clinic.state(),
                clinic.status().name(),
                clinic.createdAt(),
                clinic.updatedAt()
        );
    }
}
