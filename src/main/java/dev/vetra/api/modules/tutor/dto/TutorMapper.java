package dev.vetra.api.modules.tutor.dto;

import dev.vetra.api.modules.tutor.domain.Tutor;

import java.util.UUID;

/**
 * Static mapping between Tutor domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class TutorMapper {

    private TutorMapper() {
        // utility class
    }

    public static Tutor toDomain(UUID clinicId, CreateTutorRequest request) {
        return Tutor.create(
                clinicId,
                request.name(),
                request.phone(),
                request.email(),
                request.document(),
                request.address(),
                request.city(),
                request.state(),
                request.zipCode()
        );
    }

    public static TutorResponse toResponse(Tutor tutor) {
        return new TutorResponse(
                tutor.id(),
                tutor.clinicId(),
                tutor.name(),
                tutor.phone(),
                tutor.email(),
                tutor.document(),
                tutor.address(),
                tutor.city(),
                tutor.state(),
                tutor.zipCode(),
                tutor.createdAt(),
                tutor.updatedAt()
        );
    }
}
