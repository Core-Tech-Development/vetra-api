package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.billing.usecase.CreateBillingRecordUseCase;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
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
import static org.mockito.Mockito.*;

class IssueLaudoUseCaseTest {

    private LaudoRepository laudoRepository;
    private AppointmentRepository appointmentRepository;
    private ExamRequestRepository examRequestRepository;
    private CreateBillingRecordUseCase createBillingRecordUseCase;
    private NotificationService notificationService;
    private IssueLaudoUseCase useCase;

    private static final UUID LAUDO_ID = UUID.randomUUID();
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        laudoRepository = mock(LaudoRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        createBillingRecordUseCase = mock(CreateBillingRecordUseCase.class);
        notificationService = mock(NotificationService.class);
        useCase = new IssueLaudoUseCase(laudoRepository, appointmentRepository,
                examRequestRepository, createBillingRecordUseCase, notificationService);
    }

    @Test
    void shouldFailWhenLaudoNotFound() {
        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenLaudoIsNotDraft() {
        var laudo = Laudo.restore(LAUDO_ID, APPOINTMENT_ID, SPECIALIST_ID,
                LaudoStatus.ISSUED, "Findings", "Conclusion", null, null,
                Instant.now(), Instant.now(), Instant.now());

        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(laudo)));

        useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class);
    }

    @Test
    void shouldIssueDraftLaudo() {
        var draft = Laudo.restore(LAUDO_ID, APPOINTMENT_ID, SPECIALIST_ID,
                LaudoStatus.DRAFT, "Findings", "Conclusion", "Recommendations", null,
                null, Instant.now(), Instant.now());

        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(draft)));
        when(laudoRepository.update(any(Laudo.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Laudo) inv.getArgument(0)));
        // Side effects — all recover on failure, so mock them
        when(appointmentRepository.updateStatus(eq(APPOINTMENT_ID), eq(AppointmentStatus.REPORT_ISSUED)))
                .thenReturn(Uni.createFrom().voidItem());
        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(createBillingRecordUseCase.execute(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        var result = useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(LaudoStatus.ISSUED);
        assertThat(result.issuedAt()).isNotNull();
        assertThat(result.appointmentId()).isEqualTo(APPOINTMENT_ID);
        verify(laudoRepository).update(any(Laudo.class));
    }
}
