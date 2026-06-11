package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.domain.Specialty;
import dev.vetra.api.modules.specialist.dto.UpdateSpecialistRequest;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Updates an existing specialist's information.
 * The specialist must exist; otherwise a NotFoundException is thrown.
 */
@ApplicationScoped
public class UpdateSpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateSpecialistUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public UpdateSpecialistUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Specialist> execute(UUID id, UpdateSpecialistRequest request) {
        return specialistRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Specialist", id));
                    }

                    Specialist existing = opt.get();
                    Specialist updated = Specialist.restore(
                            existing.id(),
                            existing.userId(),
                            request.name(),
                            request.email(),
                            request.phone(),
                            request.crmv(),
                            request.crmvState(),
                            Specialty.valueOf(request.specialty()),
                            request.baseCity(),
                            request.baseState(),
                            request.maxTravelRadiusKm(),
                            request.hasOwnEquipment(),
                            request.bio(),
                            existing.status(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating specialist: id=%s", id);
                    return specialistRepository.update(updated);
                });
    }
}
