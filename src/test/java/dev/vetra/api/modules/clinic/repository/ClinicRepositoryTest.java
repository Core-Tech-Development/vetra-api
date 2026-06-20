package dev.vetra.api.modules.clinic.repository;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ClinicRepositoryTest {

    @Inject
    ClinicRepository clinicRepository;

    private Clinic sampleClinic;

    @BeforeEach
    void setUp() {
        sampleClinic = Clinic.restore(UUID.randomUUID(), "Clinica Test", "repo-test-" + UUID.randomUUID().toString().substring(0, 8),
                "clinic-repo-" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "11999999999",
                "Rua A", "SP", "SP", ClinicStatus.PENDING_APPROVAL, Instant.now(), Instant.now());
    }

    @Test
    void shouldSaveAndFindById() {
        var saved = clinicRepository.save(sampleClinic)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(saved.id()).isEqualTo(sampleClinic.id());
        assertThat(saved.name()).isEqualTo("Clinica Test");
        assertThat(saved.status()).isEqualTo(ClinicStatus.PENDING_APPROVAL);

        var found = clinicRepository.findById(saved.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
        assertThat(found.get().name()).isEqualTo("Clinica Test");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        var found = clinicRepository.findById(UUID.randomUUID())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByDocument() {
        clinicRepository.save(sampleClinic)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var found = clinicRepository.findByDocument(sampleClinic.document())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isPresent();
        assertThat(found.get().document()).isEqualTo(sampleClinic.document());
    }

    @Test
    void shouldReturnEmptyForNonExistentDocument() {
        var found = clinicRepository.findByDocument("NONEXISTENT99999")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateClinic() {
        var saved = clinicRepository.save(sampleClinic)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        var updated = Clinic.restore(saved.id(), "Updated Name", saved.document(),
                saved.email(), saved.phone(), saved.address(), saved.city(), saved.state(),
                ClinicStatus.ACTIVE, saved.createdAt(), Instant.now());

        var result = clinicRepository.update(updated)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.status()).isEqualTo(ClinicStatus.ACTIVE);
    }

    @Test
    void shouldDeleteById() {
        clinicRepository.save(sampleClinic)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var deleted = clinicRepository.deleteById(sampleClinic.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(deleted).isTrue();

        var found = clinicRepository.findById(sampleClinic.id())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistent() {
        var deleted = clinicRepository.deleteById(UUID.randomUUID())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(deleted).isFalse();
    }

    @Test
    void shouldCountClinics() {
        clinicRepository.save(sampleClinic)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        var count = clinicRepository.count()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(count).isGreaterThanOrEqualTo(1L);
    }
}
