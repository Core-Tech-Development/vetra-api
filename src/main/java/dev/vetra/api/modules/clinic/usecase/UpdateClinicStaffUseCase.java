package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.domain.ClinicStaffRole;
import dev.vetra.api.modules.clinic.dto.UpdateClinicStaffRequest;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Updates an existing clinic staff member's information.
 * Validates that the staff member exists.
 */
@ApplicationScoped
public class UpdateClinicStaffUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateClinicStaffUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public UpdateClinicStaffUseCase(ClinicStaffRepository clinicStaffRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<ClinicStaff> execute(UUID id, UpdateClinicStaffRequest request) {
        return clinicStaffRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ClinicStaff", id));
                    }

                    ClinicStaff existing = opt.get();
                    return doUpdate(existing, request);
                });
    }

    private Uni<ClinicStaff> doUpdate(ClinicStaff existing, UpdateClinicStaffRequest request) {
        ClinicStaff updated = ClinicStaff.restore(
                existing.id(),
                existing.clinicId(),
                existing.userId(),
                request.name(),
                existing.email(),
                request.phone(),
                ClinicStaffRole.valueOf(request.role()),
                existing.status(),
                existing.createdAt(),
                Instant.now()
        );
        LOG.infof("Updating clinic staff: id=%s", updated.id());
        return clinicStaffRepository.update(updated);
    }
}
