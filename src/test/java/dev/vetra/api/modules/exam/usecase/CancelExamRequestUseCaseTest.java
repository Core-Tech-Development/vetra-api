package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
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
import static org.mockito.Mockito.*;

class CancelExamRequestUseCaseTest {

    private ExamRequestRepository examRequestRepository;
    private CancelExamRequestUseCase useCase;

    private static final UUID EXAM_REQUEST_ID = UUID.randomUUID();
    private static final UUID CLINIC_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRequestRepository = mock(ExamRequestRepository.class);
        useCase = new CancelExamRequestUseCase(examRequestRepository);
    }

    @Test
    void shouldFailWhenExamRequestNotFound() {
        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(EXAM_REQUEST_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenStatusIsNotCancellable() {
        var examRequest = ExamRequest.restore(EXAM_REQUEST_ID, CLINIC_ID, PATIENT_ID,
                "ABDOMINAL_ULTRASOUND", ExamPriority.ROUTINE, null, "History",
                null, ExamRequestStatus.SCHEDULED, "user-1", Instant.now(), Instant.now());

        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(examRequest)));

        useCase.execute(EXAM_REQUEST_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class);
    }

    @Test
    void shouldCancelExamRequestFromCreatedStatus() {
        var examRequest = ExamRequest.restore(EXAM_REQUEST_ID, CLINIC_ID, PATIENT_ID,
                "ABDOMINAL_ULTRASOUND", ExamPriority.ROUTINE, null, "History",
                null, ExamRequestStatus.CREATED, "user-1", Instant.now(), Instant.now());
        var cancelled = ExamRequest.restore(EXAM_REQUEST_ID, CLINIC_ID, PATIENT_ID,
                "ABDOMINAL_ULTRASOUND", ExamPriority.ROUTINE, null, "History",
                null, ExamRequestStatus.CANCELLED, "user-1", Instant.now(), Instant.now());

        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(examRequest)));
        when(examRequestRepository.updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.CANCELLED))
                .thenReturn(Uni.createFrom().item(cancelled));

        var result = useCase.execute(EXAM_REQUEST_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(ExamRequestStatus.CANCELLED);
        verify(examRequestRepository).updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.CANCELLED);
    }

    @Test
    void shouldCancelExamRequestFromPendingSpecialistStatus() {
        var examRequest = ExamRequest.restore(EXAM_REQUEST_ID, CLINIC_ID, PATIENT_ID,
                "CARDIAC_ULTRASOUND", ExamPriority.URGENT, "Suspected issue", "History",
                null, ExamRequestStatus.PENDING_SPECIALIST, "user-1", Instant.now(), Instant.now());
        var cancelled = ExamRequest.restore(EXAM_REQUEST_ID, CLINIC_ID, PATIENT_ID,
                "CARDIAC_ULTRASOUND", ExamPriority.URGENT, "Suspected issue", "History",
                null, ExamRequestStatus.CANCELLED, "user-1", Instant.now(), Instant.now());

        when(examRequestRepository.findById(EXAM_REQUEST_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(examRequest)));
        when(examRequestRepository.updateStatus(EXAM_REQUEST_ID, ExamRequestStatus.CANCELLED))
                .thenReturn(Uni.createFrom().item(cancelled));

        var result = useCase.execute(EXAM_REQUEST_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.status()).isEqualTo(ExamRequestStatus.CANCELLED);
    }
}
