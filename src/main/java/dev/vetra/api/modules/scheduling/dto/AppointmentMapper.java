package dev.vetra.api.modules.scheduling.dto;

import dev.vetra.api.modules.scheduling.domain.Appointment;

/**
 * Static mapping between Appointment domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class AppointmentMapper {

    private AppointmentMapper() {
        // utility class
    }

    public static Appointment toDomain(CreateAppointmentRequest request) {
        return Appointment.create(
                request.examRequestId(),
                request.specialistId(),
                request.availabilitySlotId(),
                request.scheduledStartAt(),
                request.scheduledEndAt()
        );
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.id(),
                appointment.examRequestId(),
                appointment.specialistId(),
                appointment.availabilitySlotId(),
                appointment.scheduledStartAt(),
                appointment.scheduledEndAt(),
                appointment.actualStartAt(),
                appointment.actualEndAt(),
                appointment.status().name(),
                appointment.cancelReason(),
                appointment.notes(),
                appointment.createdAt(),
                appointment.updatedAt()
        );
    }
}
