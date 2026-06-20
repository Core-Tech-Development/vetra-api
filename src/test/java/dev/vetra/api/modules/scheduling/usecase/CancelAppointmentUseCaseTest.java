package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CancelAppointmentUseCaseTest {

    private AppointmentRepository appointmentRepository;
    private AvailabilitySlotRepository slotRepository;
    private ExamRequestRepository examRequestRepository;
    private NotificationService notificationService;
    private CancelAppointmentUseCase useCase;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        slotRepository = mock(AvailabilitySlotRepository.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        notificationService = mock(NotificationService.class);
        useCase = new CancelAppointmentUseCase(appointmentRepository, slotRepository, examRequestRepository, notificationService);
    }

    @Test
    void shouldFailWhenAppointmentNotFound() {
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(APPOINTMENT_ID, "reason")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenAlreadyCancelled() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID,
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.CANCELLED, "already cancelled", null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));

        useCase.execute(APPOINTMENT_ID, "reason")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "Cannot cancel");
    }

    @Test
    void shouldCancelAppointmentAndFreeSlot() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID,
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.ACCEPTED, null, null,
                Instant.now(), Instant.now());
        var slot = AvailabilitySlot.restore(SLOT_ID, SPECIALIST_ID, Instant.now(), Instant.now().plusSeconds(3600),
                SlotStatus.RESERVED, null, null, Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(slotRepository.findById(SLOT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(slot)));
        when(slotRepository.updateStatus(SLOT_ID, SlotStatus.AVAILABLE))
                .thenReturn(Uni.createFrom().voidItem());
        when(appointmentRepository.update(any(Appointment.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Appointment) inv.getArgument(0)));
        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        var result = useCase.execute(APPOINTMENT_ID, "Client request")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.cancelReason()).isEqualTo("Client request");
        verify(slotRepository).updateStatus(SLOT_ID, SlotStatus.AVAILABLE);
    }

    @Test
    void shouldCancelWithoutSlotAndRevertExamRequest() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, null,
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE, null, null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(appointmentRepository.update(any(Appointment.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Appointment) inv.getArgument(0)));
        when(examRequestRepository.updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.CREATED))
                .thenReturn(Uni.createFrom().nullItem());
        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        var result = useCase.execute(APPOINTMENT_ID, "Changed mind")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(slotRepository, never()).updateStatus(any(), any());
        verify(examRequestRepository).updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.CREATED);
    }
}
