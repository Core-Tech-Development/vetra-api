package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentRequest;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScheduleAppointmentUseCaseTest {

    private AppointmentRepository appointmentRepository;
    private AvailabilitySlotRepository slotRepository;
    private ExamRequestRepository examRequestRepository;
    private NotificationService notificationService;
    private ScheduleAppointmentUseCase useCase;

    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final Instant SLOT_START = Instant.parse("2026-07-01T09:00:00Z");
    private static final Instant SLOT_END = Instant.parse("2026-07-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        slotRepository = mock(AvailabilitySlotRepository.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        notificationService = mock(NotificationService.class);
        useCase = new ScheduleAppointmentUseCase(appointmentRepository, slotRepository, examRequestRepository, notificationService);
    }

    @Test
    void shouldFailWhenSlotIdIsNull() {
        var request = new CreateAppointmentRequest(EXAM_REQUEST_ID, SPECIALIST_ID, null, null, null);

        useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "slot is required");
    }

    @Test
    void shouldFailWhenExamRequestAlreadyHasActiveAppointment() {
        var request = new CreateAppointmentRequest(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, null, null);
        var existing = Appointment.create(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, SLOT_START, SLOT_END);

        when(appointmentRepository.findActiveByExamRequestId(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(existing)));

        useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "already has an active appointment");
    }

    @Test
    void shouldFailWhenSlotNotFound() {
        var request = new CreateAppointmentRequest(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, null, null);

        when(appointmentRepository.findActiveByExamRequestId(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(slotRepository.findById(SLOT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenSlotNotAvailable() {
        var request = new CreateAppointmentRequest(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, null, null);
        var slot = AvailabilitySlot.restore(SLOT_ID, SPECIALIST_ID, SLOT_START, SLOT_END,
                SlotStatus.RESERVED, null, null, Instant.now(), Instant.now());

        when(appointmentRepository.findActiveByExamRequestId(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(slotRepository.findById(SLOT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(slot)));
        when(slotRepository.reserveIfAvailable(SLOT_ID))
                .thenReturn(Uni.createFrom().item(false));

        useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "not available");
    }

    @Test
    void shouldCreateAppointmentAndReserveSlot() {
        var request = new CreateAppointmentRequest(EXAM_REQUEST_ID, SPECIALIST_ID, SLOT_ID, null, null);
        var slot = AvailabilitySlot.restore(SLOT_ID, SPECIALIST_ID, SLOT_START, SLOT_END,
                SlotStatus.AVAILABLE, null, null, Instant.now(), Instant.now());

        when(appointmentRepository.findActiveByExamRequestId(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(slotRepository.findById(SLOT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(slot)));
        when(slotRepository.reserveIfAvailable(SLOT_ID))
                .thenReturn(Uni.createFrom().item(true));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Appointment) inv.getArgument(0)));
        when(examRequestRepository.updateStatus(eq(EXAM_REQUEST_ID), eq(ExamRequestStatus.PENDING_SPECIALIST)))
                .thenReturn(Uni.createFrom().nullItem());
        when(notificationService.notifySpecialist(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());

        var result = useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE);
        assertThat(result.examRequestId()).isEqualTo(EXAM_REQUEST_ID);
        assertThat(result.specialistId()).isEqualTo(SPECIALIST_ID);
        assertThat(result.availabilitySlotId()).isEqualTo(SLOT_ID);
        assertThat(result.scheduledStartAt()).isEqualTo(SLOT_START);
        assertThat(result.scheduledEndAt()).isEqualTo(SLOT_END);

        verify(slotRepository).reserveIfAvailable(SLOT_ID);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(examRequestRepository).updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.PENDING_SPECIALIST);
    }
}
