package dev.vetra.api.modules.scheduling.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing an appointment between a clinic exam request and a specialist.
 * Pure Java record — no framework annotations.
 */
public record Appointment(
        UUID id,
        UUID examRequestId,
        UUID specialistId,
        UUID availabilitySlotId,
        Instant scheduledStartAt,
        Instant scheduledEndAt,
        Instant actualStartAt,
        Instant actualEndAt,
        AppointmentStatus status,
        String cancelReason,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory for creating a new appointment with WAITING_SPECIALIST_ACCEPTANCE status.
     */
    public static Appointment create(UUID examRequestId, UUID specialistId, UUID availabilitySlotId,
                                     Instant scheduledStartAt, Instant scheduledEndAt) {
        Instant now = Instant.now();
        return new Appointment(
                UUID.randomUUID(),
                examRequestId,
                specialistId,
                availabilitySlotId,
                scheduledStartAt,
                scheduledEndAt,
                null,
                null,
                AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE,
                null,
                null,
                now,
                now
        );
    }

    /**
     * Factory for restoring an appointment from persistence.
     */
    public static Appointment restore(UUID id, UUID examRequestId, UUID specialistId, UUID availabilitySlotId,
                                      Instant scheduledStartAt, Instant scheduledEndAt,
                                      Instant actualStartAt, Instant actualEndAt,
                                      AppointmentStatus status, String cancelReason, String notes,
                                      Instant createdAt, Instant updatedAt) {
        return new Appointment(id, examRequestId, specialistId, availabilitySlotId,
                scheduledStartAt, scheduledEndAt, actualStartAt, actualEndAt,
                status, cancelReason, notes, createdAt, updatedAt);
    }

    /**
     * Returns a copy of this appointment with updated status and timestamp.
     */
    public Appointment withStatus(AppointmentStatus newStatus) {
        return new Appointment(id, examRequestId, specialistId, availabilitySlotId,
                scheduledStartAt, scheduledEndAt, actualStartAt, actualEndAt,
                newStatus, cancelReason, notes, createdAt, Instant.now());
    }

    /**
     * Returns a copy with actual start time set and status updated.
     */
    public Appointment withActualStart(Instant start, AppointmentStatus newStatus) {
        return new Appointment(id, examRequestId, specialistId, availabilitySlotId,
                scheduledStartAt, scheduledEndAt, start, actualEndAt,
                newStatus, cancelReason, notes, createdAt, Instant.now());
    }

    /**
     * Returns a copy with actual end time set and status updated.
     */
    public Appointment withActualEnd(Instant end, AppointmentStatus newStatus) {
        return new Appointment(id, examRequestId, specialistId, availabilitySlotId,
                scheduledStartAt, scheduledEndAt, actualStartAt, end,
                newStatus, cancelReason, notes, createdAt, Instant.now());
    }

    /**
     * Returns a copy with cancellation reason and CANCELLED status.
     */
    public Appointment withCancellation(String reason) {
        return new Appointment(id, examRequestId, specialistId, availabilitySlotId,
                scheduledStartAt, scheduledEndAt, actualStartAt, actualEndAt,
                AppointmentStatus.CANCELLED, reason, notes, createdAt, Instant.now());
    }
}
