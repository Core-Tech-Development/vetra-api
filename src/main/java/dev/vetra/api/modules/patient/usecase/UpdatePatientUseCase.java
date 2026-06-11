package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.dto.UpdatePatientRequest;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Updates an existing patient's information.
 * Validates that the patient exists before updating.
 */
@ApplicationScoped
public class UpdatePatientUseCase {

    private static final Logger LOG = Logger.getLogger(UpdatePatientUseCase.class);

    private final PatientRepository patientRepository;

    @Inject
    public UpdatePatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Uni<Patient> execute(UUID id, UpdatePatientRequest request) {
        return patientRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Patient", id));
                    }

                    Patient existing = opt.get();

                    LocalDate birthDate = request.birthDate() != null && !request.birthDate().isBlank()
                            ? LocalDate.parse(request.birthDate())
                            : null;

                    Patient updated = Patient.restore(
                            existing.id(),
                            existing.clinicId(),
                            existing.tutorId(),
                            request.name(),
                            request.species(),
                            request.breed(),
                            request.sex(),
                            birthDate,
                            request.weightKg(),
                            request.neutered(),
                            request.microchip(),
                            request.clinicalNotes(),
                            existing.createdAt(),
                            Instant.now()
                    );
                    LOG.infof("Updating patient: id=%s", updated.id());
                    return patientRepository.update(updated);
                });
    }
}
