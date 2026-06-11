package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.dto.UpdateMyProfileRequest;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Updates the specialist's own profile.
 * Only allows changing non-sensitive fields (name, phone, baseCity, baseState,
 * maxTravelRadiusKm, hasOwnEquipment, bio).
 * Preserves: id, userId, email, crmv, crmvState, specialty, status, createdAt.
 */
@ApplicationScoped
public class UpdateMyProfileUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateMyProfileUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public UpdateMyProfileUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Specialist> execute(String userId, UpdateMyProfileRequest request) {
        return specialistRepository.findByUserId(userId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Specialist profile not found for current user"));
                    }

                    Specialist existing = opt.get();
                    Specialist updated = Specialist.restore(
                            existing.id(),
                            existing.userId(),
                            request.name(),
                            existing.email(),
                            request.phone(),
                            existing.crmv(),
                            existing.crmvState(),
                            existing.specialty(),
                            request.baseCity(),
                            request.baseState(),
                            request.maxTravelRadiusKm(),
                            request.hasOwnEquipment(),
                            request.bio(),
                            existing.status(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating own profile: userId=%s, specialistId=%s", userId, existing.id());
                    return specialistRepository.update(updated);
                });
    }
}
