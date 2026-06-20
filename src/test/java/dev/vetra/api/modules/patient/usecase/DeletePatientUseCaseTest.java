package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DeletePatientUseCaseTest {

    private PatientRepository patientRepository;
    private ExamRequestRepository examRequestRepository;
    private DeletePatientUseCase useCase;

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        examRequestRepository = mock(ExamRequestRepository.class);
        useCase = new DeletePatientUseCase(patientRepository, examRequestRepository);
    }

    @Test
    void shouldFailWhenPatientNotFound() {
        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(PATIENT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenPatientHasExamRequests() {
        var patient = Patient.create(UUID.randomUUID(), UUID.randomUUID(), "Rex", "DOG", "Labrador",
                "MALE", null, null, false, null, null);
        var examRequest = ExamRequest.create(UUID.randomUUID(), PATIENT_ID, "ABDOMINAL_ULTRASOUND",
                ExamPriority.ROUTINE, null, "History", null, "user-1");
        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(patient)));
        when(examRequestRepository.findByPatientId(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(List.of(examRequest)));

        useCase.execute(PATIENT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "exam request");
    }

    @Test
    void shouldDeletePatientSuccessfully() {
        var patient = Patient.create(UUID.randomUUID(), UUID.randomUUID(), "Rex", "DOG", "Labrador",
                "MALE", null, null, false, null, null);
        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(patient)));
        when(examRequestRepository.findByPatientId(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));
        when(patientRepository.deleteById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(true));

        useCase.execute(PATIENT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        verify(patientRepository).deleteById(PATIENT_ID);
    }
}
