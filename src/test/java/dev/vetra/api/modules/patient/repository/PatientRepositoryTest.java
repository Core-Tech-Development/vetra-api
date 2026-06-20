package dev.vetra.api.modules.patient.repository;

import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.shared.TestDataFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PatientRepositoryTest {

    @Inject
    PatientRepository patientRepository;

    @Inject
    TestDataFactory testDataFactory;

    private UUID clinicId;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        tutorId = UUID.randomUUID();

        testDataFactory.insertClinic(clinicId, "Test Clinic", "pat-repo-" + UUID.randomUUID().toString().substring(0, 6), "ACTIVE")
                .flatMap(cid -> testDataFactory.insertTutor(tutorId, cid, "Test Tutor"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    void shouldSaveAndFindById() {
        var patient = Patient.create(clinicId, tutorId, "Rex", "DOG", "Labrador",
                "MALE", null, BigDecimal.valueOf(25.5), false, null, null);

        var saved = patientRepository.save(patient)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(saved.id()).isEqualTo(patient.id());
        assertThat(saved.name()).isEqualTo("Rex");
        assertThat(saved.species()).isEqualTo("DOG");
        assertThat(saved.clinicId()).isEqualTo(clinicId);
        assertThat(saved.tutorId()).isEqualTo(tutorId);

        var found = patientRepository.findById(saved.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Rex");
        assertThat(found.get().breed()).isEqualTo("Labrador");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        var found = patientRepository.findById(UUID.randomUUID())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdatePatient() {
        var patient = Patient.create(clinicId, tutorId, "Old Name", "CAT", "Persian",
                "FEMALE", null, BigDecimal.valueOf(4.0), true, null, null);

        patientRepository.save(patient)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var updated = Patient.restore(patient.id(), clinicId, tutorId, "New Name", "CAT", "Persian",
                "FEMALE", null, BigDecimal.valueOf(5.0), true, "CHIP-123", "Updated notes",
                patient.createdAt(), java.time.Instant.now());

        var result = patientRepository.update(updated)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.weightKg()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        assertThat(result.microchip()).isEqualTo("CHIP-123");
        assertThat(result.clinicalNotes()).isEqualTo("Updated notes");
    }

    @Test
    void shouldDeleteById() {
        var patient = Patient.create(clinicId, tutorId, "ToDelete", "DOG", "Beagle",
                "MALE", null, null, false, null, null);

        patientRepository.save(patient)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var deleted = patientRepository.deleteById(patient.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(deleted).isTrue();

        var found = patientRepository.findById(patient.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByClinicIdWithPagination() {
        var p1 = Patient.create(clinicId, tutorId, "Patient 1", "DOG", "Poodle", "MALE", null, null, false, null, null);
        var p2 = Patient.create(clinicId, tutorId, "Patient 2", "CAT", "Siamese", "FEMALE", null, null, false, null, null);

        patientRepository.save(p1)
                .flatMap(x -> patientRepository.save(p2))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var page = patientRepository.findByClinicId(clinicId, 0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(page).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldCountByClinicId() {
        var patient = Patient.create(clinicId, tutorId, "CountMe", "DOG", "Husky", "MALE", null, null, false, null, null);

        patientRepository.save(patient)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var count = patientRepository.countByClinicId(clinicId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(count).isGreaterThanOrEqualTo(1L);
    }
}
