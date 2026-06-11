package dev.vetra.api.modules.tutor.usecase;

import dev.vetra.api.modules.tutor.domain.Tutor;
import dev.vetra.api.modules.tutor.dto.UpdateTutorRequest;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Updates an existing tutor's information.
 * Validates that the tutor exists and that no duplicate document exists within the same clinic.
 */
@ApplicationScoped
public class UpdateTutorUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateTutorUseCase.class);

    private final TutorRepository tutorRepository;

    @Inject
    public UpdateTutorUseCase(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public Uni<Tutor> execute(UUID id, UpdateTutorRequest request) {
        return tutorRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Tutor", id));
                    }

                    Tutor existing = opt.get();

                    if (request.document() != null && !request.document().isBlank()
                            && !request.document().equals(existing.document())) {
                        return tutorRepository.findByDocumentAndClinicId(request.document(), existing.clinicId())
                                .flatMap(duplicate -> {
                                    if (duplicate.isPresent()) {
                                        return Uni.createFrom().failure(
                                                new DuplicateException("Tutor", "document", request.document())
                                        );
                                    }
                                    return doUpdate(existing, request);
                                });
                    }

                    return doUpdate(existing, request);
                });
    }

    private Uni<Tutor> doUpdate(Tutor existing, UpdateTutorRequest request) {
        Tutor updated = Tutor.restore(
                existing.id(),
                existing.clinicId(),
                request.name(),
                request.phone(),
                request.email(),
                request.document(),
                request.address(),
                request.city(),
                request.state(),
                request.zipCode(),
                existing.createdAt(),
                Instant.now()
        );
        LOG.infof("Updating tutor: id=%s", updated.id());
        return tutorRepository.update(updated);
    }
}
