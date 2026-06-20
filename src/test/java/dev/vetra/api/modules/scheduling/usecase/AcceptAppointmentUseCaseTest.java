package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AcceptAppointmentUseCaseTest {

    private AppointmentRepository appointmentRepository;
    private ExamRequestRepository examRequestRepository;
    private AppointmentOwnershipValidator ownershipValidator;
    private NotificationService notificationService;
    private LogAuditEventUseCase auditUseCase;
    private AcceptAppointmentUseCase useCase;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();
    private static final UUID CLINIC_ID = UUID.randomUUID();
    private static final String CALLER_USER_ID = "user-123";
    private static final Set<String> ADMIN_ROLES = Set.of("PLATFORM_ADMIN");

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        ownershipValidator = mock(AppointmentOwnershipValidator.class);
        notificationService = mock(NotificationService.class);
        auditUseCase = mock(LogAuditEventUseCase.class);
        useCase = new AcceptAppointmentUseCase(appointmentRepository, examRequestRepository,
                ownershipValidator, notificationService, auditUseCase);
    }

    private Appointment waitingAppointment() {
        return Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200),
                null, null, AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void shouldFailWhenAppointmentNotFound() {
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(APPOINTMENT_ID, CALLER_USER_ID, ADMIN_ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenStatusDoesNotAllowAcceptance() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.IN_TRANSIT, null, null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(ownershipValidator.validate(any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());

        useCase.execute(APPOINTMENT_ID, CALLER_USER_ID, ADMIN_ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "Cannot accept");
    }

    @Test
    void shouldAcceptAppointmentAndUpdateExamRequestStatus() {
        var appointment = waitingAppointment();

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(ownershipValidator.validate(any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());
        when(appointmentRepository.update(any(Appointment.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Appointment) inv.getArgument(0)));
        when(examRequestRepository.updateStatus(eq(EXAM_REQUEST_ID), eq(ExamRequestStatus.SPECIALIST_ASSIGNED)))
                .thenReturn(Uni.createFrom().nullItem());
        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(auditUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        var result = useCase.execute(APPOINTMENT_ID, CALLER_USER_ID, ADMIN_ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(AppointmentStatus.ACCEPTED);
        verify(examRequestRepository).updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.SPECIALIST_ASSIGNED);
    }

    @Test
    void shouldFailWhenAlreadyAccepted() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.ACCEPTED, null, null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(ownershipValidator.validate(any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());

        useCase.execute(APPOINTMENT_ID, CALLER_USER_ID, ADMIN_ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "Cannot accept");
    }
}
