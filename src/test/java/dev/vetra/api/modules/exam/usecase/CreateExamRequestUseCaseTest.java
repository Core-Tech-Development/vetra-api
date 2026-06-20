package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.dto.CreateExamRequestRequest;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateExamRequestUseCaseTest {

    private ExamRequestRepository examRequestRepository;
    private PatientRepository patientRepository;
    private NotificationService notificationService;
    private LogAuditEventUseCase auditUseCase;
    private CreateExamRequestUseCase useCase;

    private static final UUID CLINIC_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRequestRepository = mock(ExamRequestRepository.class);
        patientRepository = mock(PatientRepository.class);
        notificationService = mock(NotificationService.class);
        auditUseCase = mock(LogAuditEventUseCase.class);
        useCase = new CreateExamRequestUseCase(examRequestRepository, patientRepository, notificationService, auditUseCase);
    }

    @Test
    void shouldFailWhenPatientNotFound() {
        var request = new CreateExamRequestRequest(PATIENT_ID, "ABDOMINAL_ULTRASOUND",
                "ROUTINE", null, "Clinical history", null);

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(CLINIC_ID, request, "user-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldCreateExamRequestWithCreatedStatus() {
        var request = new CreateExamRequestRequest(PATIENT_ID, "ABDOMINAL_ULTRASOUND",
                "ROUTINE", "Suspected mass", "Clinical history", null);
        var patient = Patient.create(CLINIC_ID, UUID.randomUUID(), "Rex", "DOG", "Labrador",
                "MALE", null, null, false, null, null);

        when(patientRepository.findById(PATIENT_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(patient)));
        when(examRequestRepository.save(any(ExamRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item((ExamRequest) inv.getArgument(0)));
        when(notificationService.notifyAllActiveSpecialists(any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().voidItem());
        when(auditUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        var result = useCase.execute(CLINIC_ID, request, "user-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(ExamRequestStatus.CREATED);
        assertThat(result.clinicId()).isEqualTo(CLINIC_ID);
        assertThat(result.patientId()).isEqualTo(PATIENT_ID);
        assertThat(result.examType()).isEqualTo("ABDOMINAL_ULTRASOUND");
        verify(examRequestRepository).save(any(ExamRequest.class));
    }
}
