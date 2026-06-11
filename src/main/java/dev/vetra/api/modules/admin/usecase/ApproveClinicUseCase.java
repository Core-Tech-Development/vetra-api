package dev.vetra.api.modules.admin.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Approves a clinic that is in PENDING_APPROVAL status.
 * Changes clinic status to ACTIVE.
 */
@ApplicationScoped
public class ApproveClinicUseCase {

    private static final Logger LOG = Logger.getLogger(ApproveClinicUseCase.class);

    private final ClinicRepository clinicRepository;

    @Inject
    public ApproveClinicUseCase(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public Uni<Clinic> execute(UUID id) {
        return clinicRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Clinic", id)))
                .flatMap(clinic -> {
                    if (clinic.status() != ClinicStatus.PENDING_APPROVAL) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Clinic must be in PENDING_APPROVAL status to approve"));
                    }

                    Clinic approved = Clinic.restore(
                            clinic.id(), clinic.name(), clinic.document(),
                            clinic.email(), clinic.phone(), clinic.address(),
                            clinic.city(), clinic.state(),
                            ClinicStatus.ACTIVE, clinic.createdAt(), Instant.now());

                    LOG.infof("Approving clinic: id=%s", id);
                    return clinicRepository.update(approved);
                });
    }
}
