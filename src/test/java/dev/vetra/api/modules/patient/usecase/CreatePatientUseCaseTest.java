package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.dto.CreatePatientRequest;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.modules.tutor.domain.Tutor;
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

class CreatePatientUseCaseTest {

    private PatientRepository patientRepository;
    private TutorRepository tutorRepository;
    private ClinicRepository clinicRepository;
    private CreatePatientUseCase useCase;

    private static final UUID CLINIC_ID = UUID.randomUUID();
    private static final UUID TUTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        tutorRepository = mock(TutorRepository.class);
        clinicRepository = mock(ClinicRepository.class);
        useCase = new CreatePatientUseCase(patientRepository, tutorRepository, clinicRepository);
    }

    @Test
    void shouldFailWhenClinicNotFound() {
        var request = new CreatePatientRequest("Rex", "DOG", "Labrador", "MALE", null, null, null, null, null);

        when(clinicRepository.findById(CLINIC_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(CLINIC_ID, TUTOR_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenClinicNotActive() {
        var request = new CreatePatientRequest("Rex", "DOG", "Labrador", "MALE", null, null, null, null, null);
        var clinic = Clinic.restore(CLINIC_ID, "Test Clinic", "12345678000199",
                "test@test.com", "11999999999", null, "SP", "SP",
                ClinicStatus.PENDING_APPROVAL, Instant.now(), Instant.now());

        when(clinicRepository.findById(CLINIC_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(clinic)));

        useCase.execute(CLINIC_ID, TUTOR_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class, "ACTIVE");
    }

    @Test
    void shouldFailWhenTutorNotFound() {
        var request = new CreatePatientRequest("Rex", "DOG", "Labrador", "MALE", null, null, null, null, null);
        var clinic = Clinic.restore(CLINIC_ID, "Test Clinic", "12345678000199",
                "test@test.com", "11999999999", null, "SP", "SP",
                ClinicStatus.ACTIVE, Instant.now(), Instant.now());

        when(clinicRepository.findById(CLINIC_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(clinic)));
        when(tutorRepository.findById(TUTOR_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(CLINIC_ID, TUTOR_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldCreatePatientSuccessfully() {
        var request = new CreatePatientRequest("Rex", "DOG", "Labrador", "MALE", null, null, null, null, null);
        var clinic = Clinic.restore(CLINIC_ID, "Test Clinic", "12345678000199",
                "test@test.com", "11999999999", null, "SP", "SP",
                ClinicStatus.ACTIVE, Instant.now(), Instant.now());

        when(clinicRepository.findById(CLINIC_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(clinic)));
        var tutor = Tutor.restore(TUTOR_ID, CLINIC_ID, "Test Tutor", "11999999999",
                "tutor@test.com", null, null, null, null, null,
                java.time.Instant.now(), java.time.Instant.now());
        when(tutorRepository.findById(TUTOR_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(tutor)));
        when(patientRepository.save(any(Patient.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Patient) inv.getArgument(0)));

        var result = useCase.execute(CLINIC_ID, TUTOR_ID, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("Rex");
        assertThat(result.species()).isEqualTo("DOG");
        assertThat(result.clinicId()).isEqualTo(CLINIC_ID);
        assertThat(result.tutorId()).isEqualTo(TUTOR_ID);
        verify(patientRepository).save(any(Patient.class));
    }
}
