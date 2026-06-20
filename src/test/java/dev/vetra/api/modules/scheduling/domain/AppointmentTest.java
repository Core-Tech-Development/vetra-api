package dev.vetra.api.modules.scheduling.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-07-01T09:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T10:00:00Z");

    @Test
    void createShouldSetWaitingSpecialistAcceptanceStatus() {
        Appointment appointment = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, START, END);

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE);
        assertThat(appointment.id()).isNotNull();
        assertThat(appointment.examRequestId()).isEqualTo(EXAM_REQUEST_ID);
        assertThat(appointment.specialistId()).isEqualTo(SPECIALIST_ID);
        assertThat(appointment.availabilitySlotId()).isEqualTo(SLOT_ID);
        assertThat(appointment.scheduledStartAt()).isEqualTo(START);
        assertThat(appointment.scheduledEndAt()).isEqualTo(END);
        assertThat(appointment.actualStartAt()).isNull();
        assertThat(appointment.actualEndAt()).isNull();
        assertThat(appointment.cancelReason()).isNull();
        assertThat(appointment.createdAt()).isNotNull();
        assertThat(appointment.updatedAt()).isNotNull();
    }

    @Test
    void withStatusShouldChangeStatusAndUpdateTimestamp() {
        Appointment original = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, START, END);
        Instant originalUpdatedAt = original.updatedAt();

        Appointment accepted = original.withStatus(AppointmentStatus.ACCEPTED);

        assertThat(accepted.status()).isEqualTo(AppointmentStatus.ACCEPTED);
        assertThat(accepted.id()).isEqualTo(original.id());
        assertThat(accepted.examRequestId()).isEqualTo(original.examRequestId());
        assertThat(accepted.updatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void withActualStartShouldSetStartTimeAndStatus() {
        Appointment appointment = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, START, END);
        Instant actualStart = Instant.now();

        Appointment started = appointment.withActualStart(actualStart, AppointmentStatus.IN_SERVICE);

        assertThat(started.status()).isEqualTo(AppointmentStatus.IN_SERVICE);
        assertThat(started.actualStartAt()).isEqualTo(actualStart);
        assertThat(started.actualEndAt()).isNull();
    }

    @Test
    void withActualEndShouldSetEndTimeAndStatus() {
        Appointment appointment = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, START, END);
        Instant actualEnd = Instant.now();

        Appointment completed = appointment.withActualEnd(actualEnd, AppointmentStatus.WAITING_REPORT);

        assertThat(completed.status()).isEqualTo(AppointmentStatus.WAITING_REPORT);
        assertThat(completed.actualEndAt()).isEqualTo(actualEnd);
    }

    @Test
    void withCancellationShouldSetCancelledStatusAndReason() {
        Appointment appointment = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, START, END);
        String reason = "Client requested cancellation";

        Appointment cancelled = appointment.withCancellation(reason);

        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.cancelReason()).isEqualTo(reason);
        assertThat(cancelled.id()).isEqualTo(appointment.id());
    }

    @Test
    void restoreShouldPreserveAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant start = Instant.now().plusSeconds(3600);

        Appointment restored = Appointment.restore(
                id, EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID,
                START, END, start, null,
                AppointmentStatus.IN_SERVICE, null, "some notes",
                now, now
        );

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.examRequestId()).isEqualTo(EXAM_REQUEST_ID);
        assertThat(restored.specialistId()).isEqualTo(SPECIALIST_ID);
        assertThat(restored.availabilitySlotId()).isEqualTo(SLOT_ID);
        assertThat(restored.actualStartAt()).isEqualTo(start);
        assertThat(restored.status()).isEqualTo(AppointmentStatus.IN_SERVICE);
        assertThat(restored.notes()).isEqualTo("some notes");
    }
}
