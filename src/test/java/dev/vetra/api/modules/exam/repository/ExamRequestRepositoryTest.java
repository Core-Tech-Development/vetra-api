package dev.vetra.api.modules.exam.repository;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.shared.TestDataFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ExamRequestRepositoryTest {

    @Inject
    ExamRequestRepository examRequestRepository;

    @Inject
    TestDataFactory testDataFactory;

    private UUID clinicId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        var tutorId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        testDataFactory.insertClinic(clinicId, "Test Clinic", "exam-repo-" + UUID.randomUUID().toString().substring(0, 5), "ACTIVE")
                .flatMap(cid -> testDataFactory.insertTutor(tutorId, cid, "Test Tutor"))
                .flatMap(tid -> {
                    var patient = dev.vetra.api.modules.patient.domain.Patient.create(clinicId, tutorId,
                            "Rex", "DOG", "Labrador", "MALE", null, null, false, null, null);
                    // Use the factory-assigned id for the patient
                    patientId = patient.id();
                    var patientRepo = io.quarkus.arc.Arc.container().instance(
                            dev.vetra.api.modules.patient.repository.PatientRepository.class).get();
                    return patientRepo.save(patient);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    void shouldSaveAndFindById() {
        var examRequest = ExamRequest.create(clinicId, patientId, "ABDOMINAL_ULTRASOUND",
                ExamPriority.ROUTINE, null, "Clinical history", null, "user-1");

        var saved = examRequestRepository.save(examRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(saved.id()).isEqualTo(examRequest.id());
        assertThat(saved.status()).isEqualTo(ExamRequestStatus.CREATED);
        assertThat(saved.clinicId()).isEqualTo(clinicId);
        assertThat(saved.patientId()).isEqualTo(patientId);
        assertThat(saved.examType()).isEqualTo("ABDOMINAL_ULTRASOUND");

        var found = examRequestRepository.findById(saved.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        var found = examRequestRepository.findById(UUID.randomUUID())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateStatus() {
        var examRequest = ExamRequest.create(clinicId, patientId, "GESTATIONAL_ULTRASOUND",
                ExamPriority.URGENT, "Suspected pregnancy", "History", null, "user-1");

        examRequestRepository.save(examRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var updated = examRequestRepository.updateStatus(examRequest.id(), ExamRequestStatus.CANCELLED)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(updated.status()).isEqualTo(ExamRequestStatus.CANCELLED);
        assertThat(updated.id()).isEqualTo(examRequest.id());
    }

    @Test
    void shouldFindByPatientId() {
        var er1 = ExamRequest.create(clinicId, patientId, "ABDOMINAL_ULTRASOUND",
                ExamPriority.ROUTINE, null, "History 1", null, "user-1");
        var er2 = ExamRequest.create(clinicId, patientId, "MUSCULOSKELETAL_ULTRASOUND",
                ExamPriority.PRIORITY, null, "History 2", null, "user-1");

        examRequestRepository.save(er1)
                .flatMap(x -> examRequestRepository.save(er2))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var results = examRequestRepository.findByPatientId(patientId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldFindByClinicIdWithPagination() {
        var examRequest = ExamRequest.create(clinicId, patientId, "ABDOMINAL_ULTRASOUND",
                ExamPriority.ROUTINE, null, "History", null, "user-1");

        examRequestRepository.save(examRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var results = examRequestRepository.findByClinicId(clinicId, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
    }
}
