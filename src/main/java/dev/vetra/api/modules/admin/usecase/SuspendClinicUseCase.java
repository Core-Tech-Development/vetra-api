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
 * Suspends a clinic that is in ACTIVE status.
 * Changes clinic status to SUSPENDED.
 */
@ApplicationScoped
public class SuspendClinicUseCase {

    private static final Logger LOG = Logger.getLogger(SuspendClinicUseCase.class);

    private final ClinicRepository clinicRepository;

    @Inject
    public SuspendClinicUseCase(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public Uni<Clinic> execute(UUID id) {
        return clinicRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Clinic", id)))
                .flatMap(clinic -> {
                    if (clinic.status() != ClinicStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Clinic must be in ACTIVE status to suspend"));
                    }

                    Clinic suspended = Clinic.restore(
                            clinic.id(), clinic.name(), clinic.document(),
                            clinic.email(), clinic.phone(), clinic.address(),
                            clinic.city(), clinic.state(),
                            ClinicStatus.SUSPENDED, clinic.createdAt(), Instant.now());

                    LOG.infof("Suspending clinic: id=%s", id);
                    return clinicRepository.update(suspended);
                });
    }
}
