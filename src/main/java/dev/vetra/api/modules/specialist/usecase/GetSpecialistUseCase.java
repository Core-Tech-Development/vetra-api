package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a specialist by its unique identifier.
 */
@ApplicationScoped
public class GetSpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(GetSpecialistUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public GetSpecialistUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Specialist> execute(UUID id) {
        LOG.debugf("Fetching specialist: id=%s", id);
        return specialistRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Specialist", id)));
    }
}
