package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a clinic by its unique identifier.
 */
@ApplicationScoped
public class GetClinicUseCase {

    private static final Logger LOG = Logger.getLogger(GetClinicUseCase.class);

    private final ClinicRepository clinicRepository;

    @Inject
    public GetClinicUseCase(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public Uni<Clinic> execute(UUID id) {
        LOG.debugf("Fetching clinic: id=%s", id);
        return clinicRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Clinic", id)));
    }
}
