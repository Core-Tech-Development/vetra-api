package dev.vetra.api.modules.specialist.dto;

import dev.vetra.api.modules.specialist.domain.CoverageArea;
import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.domain.Specialty;

/**
 * Static mapping between Specialist domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class SpecialistMapper {

    private SpecialistMapper() {
        // utility class
    }

    public static Specialist toDomain(CreateSpecialistRequest request, String userId) {
        return Specialist.create(
                userId,
                request.name(),
                request.email(),
                request.phone(),
                request.crmv(),
                request.crmvState(),
                Specialty.valueOf(request.specialty()),
                request.baseCity(),
                request.baseState(),
                request.maxTravelRadiusKm(),
                request.hasOwnEquipment(),
                request.bio()
        );
    }

    public static SpecialistResponse toResponse(Specialist specialist) {
        return new SpecialistResponse(
                specialist.id(),
                specialist.userId(),
                specialist.name(),
                specialist.email(),
                specialist.phone(),
                specialist.crmv(),
                specialist.crmvState(),
                specialist.specialty().name(),
                specialist.baseCity(),
                specialist.baseState(),
                specialist.maxTravelRadiusKm(),
                specialist.hasOwnEquipment(),
                specialist.bio(),
                specialist.status().name(),
                specialist.createdAt(),
                specialist.updatedAt()
        );
    }

    public static CoverageAreaResponse toResponse(CoverageArea area) {
        return new CoverageAreaResponse(
                area.id(),
                area.specialistId(),
                area.city(),
                area.state(),
                area.radiusKm(),
                area.active(),
                area.createdAt()
        );
    }
}
