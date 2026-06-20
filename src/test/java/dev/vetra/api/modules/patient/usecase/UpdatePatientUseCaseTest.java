package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.dto.UpdatePatientRequest;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdatePatientUseCaseTest {

    private PatientRepository patientRepository;
    private UpdatePatientUseCase useCase;

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        useCase = new UpdatePatientUseCase(patientRepository);
    }

    @Test
    void shouldFailWhenPatientNotFound() {
        var request = new UpdatePatientRequest("Rex", "DOG", "Labrador", "MALE",
                null, null, null, null, null);

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(PATIENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldUpdatePatientSuccessfully() {
        var existing = Patient.restore(PATIENT_ID, UUID.randomUUID(), UUID.randomUUID(),
                "Old Name", "DOG", "Poodle", "MALE", null, BigDecimal.valueOf(10.0),
                false, null, null, Instant.now(), Instant.now());
        var request = new UpdatePatientRequest("New Name", "DOG", "Poodle", "MALE",
                "2020-01-15", BigDecimal.valueOf(12.0), true, null, "Updated notes");

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(existing)));
        when(patientRepository.update(any(Patient.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Patient) inv.getArgument(0)));

        var result = useCase.execute(PATIENT_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.weightKg()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
        assertThat(result.clinicalNotes()).isEqualTo("Updated notes");
        assertThat(result.id()).isEqualTo(PATIENT_ID);
        assertThat(result.clinicId()).isEqualTo(existing.clinicId());
    }
}
