package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Retrieves the specialist profile for the currently authenticated user.
 */
@ApplicationScoped
public class GetMyProfileUseCase {

    private static final Logger LOG = Logger.getLogger(GetMyProfileUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public GetMyProfileUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Specialist> execute(String userId) {
        LOG.infof("Fetching profile for userId=%s", userId);
        return specialistRepository.findByUserId(userId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Specialist profile not found for current user"));
                    }
                    return Uni.createFrom().item(opt.get());
                });
    }
}
