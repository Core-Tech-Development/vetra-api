package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.domain.SpecialistStatus;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Approves a specialist by changing status from PENDING_APPROVAL to ACTIVE.
 * Only specialists in PENDING_APPROVAL status can be approved.
 */
@ApplicationScoped
public class ApproveSpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(ApproveSpecialistUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public ApproveSpecialistUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Specialist> execute(UUID id) {
        return specialistRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Specialist", id));
                    }

                    Specialist existing = opt.get();

                    if (existing.status() != SpecialistStatus.PENDING_APPROVAL) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Specialist can only be approved from PENDING_APPROVAL status. Current status: " + existing.status())
                        );
                    }

                    Specialist approved = Specialist.restore(
                            existing.id(),
                            existing.userId(),
                            existing.name(),
                            existing.email(),
                            existing.phone(),
                            existing.crmv(),
                            existing.crmvState(),
                            existing.specialty(),
                            existing.baseCity(),
                            existing.baseState(),
                            existing.maxTravelRadiusKm(),
                            existing.hasOwnEquipment(),
                            existing.bio(),
                            SpecialistStatus.ACTIVE,
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Approving specialist: id=%s", id);
                    return specialistRepository.update(approved);
                });
    }
}
