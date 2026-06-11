package dev.vetra.api.modules.clinic.dto;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.domain.ClinicStaffRole;

import java.util.UUID;

public final class ClinicStaffMapper {
    private ClinicStaffMapper() {}

    public static ClinicStaff toDomain(UUID clinicId, CreateClinicStaffRequest request) {
        return ClinicStaff.create(
                clinicId,
                request.name(),
                request.email(),
                request.phone(),
                ClinicStaffRole.valueOf(request.role())
        );
    }

    public static ClinicStaffResponse toResponse(ClinicStaff staff) {
        return new ClinicStaffResponse(
                staff.id(),
                staff.clinicId(),
                staff.userId(),
                staff.name(),
                staff.email(),
                staff.phone(),
                staff.role().name(),
                staff.status().name(),
                staff.createdAt(),
                staff.updatedAt()
        );
    }
}
