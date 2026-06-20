package dev.vetra.api.modules.scheduling.usecase;

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
import static org.mockito.Mockito.*;

class StartTransitUseCaseTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentOwnershipValidator ownershipValidator;
    private ExamRequestRepository examRequestRepository;
    private NotificationService notificationService;
    private LogAuditEventUseCase auditUseCase;
    private StartTransitUseCase useCase;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();
    private static final String CALLER = "user-1";
    private static final Set<String> ROLES = Set.of("PLATFORM_ADMIN");

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        ownershipValidator = mock(AppointmentOwnershipValidator.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        notificationService = mock(NotificationService.class);
        auditUseCase = mock(LogAuditEventUseCase.class);
        useCase = new StartTransitUseCase(appointmentRepository, ownershipValidator,
                examRequestRepository, notificationService, auditUseCase);
    }

    @Test
    void shouldFailWhenNotFound() {
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(APPOINTMENT_ID, CALLER, ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenNotInAcceptedStatus() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, null,
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE, null, null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(ownershipValidator.validate(any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());

        useCase.execute(APPOINTMENT_ID, CALLER, ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "Cannot start transit");
    }

    @Test
    void shouldTransitionToInTransit() {
        var appointment = Appointment.restore(APPOINTMENT_ID, EXAM_REQUEST_ID, SPECIALIST_ID, null,
                Instant.now(), Instant.now().plusSeconds(3600),
                null, null, AppointmentStatus.ACCEPTED, null, null,
                Instant.now(), Instant.now());

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(ownershipValidator.validate(any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());
        when(appointmentRepository.update(any(Appointment.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Appointment) inv.getArgument(0)));
        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(auditUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        var result = useCase.execute(APPOINTMENT_ID, CALLER, ROLES)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(AppointmentStatus.IN_TRANSIT);
    }
}
