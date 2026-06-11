package dev.vetra.api.modules.tutor.usecase;

import dev.vetra.api.modules.tutor.domain.Tutor;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a tutor by its unique identifier.
 */
@ApplicationScoped
public class GetTutorUseCase {

    private static final Logger LOG = Logger.getLogger(GetTutorUseCase.class);

    private final TutorRepository tutorRepository;

    @Inject
    public GetTutorUseCase(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public Uni<Tutor> execute(UUID id) {
        LOG.debugf("Fetching tutor: id=%s", id);
        return tutorRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Tutor", id)));
    }
}
