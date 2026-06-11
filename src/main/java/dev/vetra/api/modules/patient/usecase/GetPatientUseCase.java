package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a patient by its unique identifier.
 */
@ApplicationScoped
public class GetPatientUseCase {

    private static final Logger LOG = Logger.getLogger(GetPatientUseCase.class);

    private final PatientRepository patientRepository;

    @Inject
    public GetPatientUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Uni<Patient> execute(UUID id) {
        LOG.debugf("Fetching patient: id=%s", id);
        return patientRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Patient", id)));
    }
}
