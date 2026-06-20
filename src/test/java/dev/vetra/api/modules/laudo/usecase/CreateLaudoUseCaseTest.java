package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.dto.CreateLaudoRequest;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import java.time.Instant;
import dev.vetra.api.shared.exception.DuplicateException;
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

class CreateLaudoUseCaseTest {

    private LaudoRepository laudoRepository;
    private AppointmentRepository appointmentRepository;
    private CreateLaudoUseCase useCase;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID SPECIALIST_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        laudoRepository = mock(LaudoRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        useCase = new CreateLaudoUseCase(laudoRepository, appointmentRepository);
    }

    @Test
    void shouldFailWhenAppointmentNotFound() {
        var request = new CreateLaudoRequest(null, null, null);

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(APPOINTMENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenLaudoAlreadyExistsForAppointment() {
        var request = new CreateLaudoRequest(null, null, null);
        var appointment = createAppointment(APPOINTMENT_ID, SPECIALIST_ID);
        var existingLaudo = Laudo.createDraft(APPOINTMENT_ID, SPECIALIST_ID);

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(laudoRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(existingLaudo)));

        useCase.execute(APPOINTMENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(DuplicateException.class);
    }

    @Test
    void shouldCreateDraftLaudoWithoutContent() {
        var request = new CreateLaudoRequest(null, null, null);
        var appointment = createAppointment(APPOINTMENT_ID, SPECIALIST_ID);

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(laudoRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(laudoRepository.save(any(Laudo.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Laudo) inv.getArgument(0)));

        var result = useCase.execute(APPOINTMENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(LaudoStatus.DRAFT);
        assertThat(result.appointmentId()).isEqualTo(APPOINTMENT_ID);
        assertThat(result.specialistId()).isEqualTo(SPECIALIST_ID);
        assertThat(result.findings()).isNull();
        assertThat(result.conclusion()).isNull();
        verify(laudoRepository).save(any(Laudo.class));
    }

    @Test
    void shouldCreateDraftLaudoWithInitialContent() {
        var request = new CreateLaudoRequest("Initial findings", "Preliminary conclusion", "Follow-up recommended");
        var appointment = createAppointment(APPOINTMENT_ID, SPECIALIST_ID);

        when(appointmentRepository.findById(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(appointment)));
        when(laudoRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(laudoRepository.save(any(Laudo.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Laudo) inv.getArgument(0)));

        var result = useCase.execute(APPOINTMENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(LaudoStatus.DRAFT);
        assertThat(result.findings()).isEqualTo("Initial findings");
        assertThat(result.conclusion()).isEqualTo("Preliminary conclusion");
        assertThat(result.recommendations()).isEqualTo("Follow-up recommended");
    }

    private Appointment createAppointment(UUID appointmentId, UUID specialistId) {
        return Appointment.restore(appointmentId, UUID.randomUUID(), specialistId, null,
                Instant.now(), Instant.now().plusSeconds(3600), null, null,
                AppointmentStatus.IN_SERVICE, null, null, Instant.now(), Instant.now());
    }
}
