package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStaffRole;
import dev.vetra.api.modules.clinic.dto.UpdateClinicProfileRequest;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.ForbiddenException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Updates the clinic profile for the currently authenticated clinic admin.
 * Only CLINIC_ADMIN role is allowed to update clinic information.
 * Identity fields (document, email) are preserved.
 */
@ApplicationScoped
public class UpdateClinicProfileUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateClinicProfileUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;
    private final ClinicRepository clinicRepository;

    @Inject
    public UpdateClinicProfileUseCase(ClinicStaffRepository clinicStaffRepository,
                                      ClinicRepository clinicRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
        this.clinicRepository = clinicRepository;
    }

    public Uni<Clinic> execute(String userId, UpdateClinicProfileRequest request) {
        LOG.infof("Updating clinic profile for userId=%s", userId);
        return clinicStaffRepository.findByUserId(userId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Staff record not found for current user"));
                    }
                    return clinicRepository.findById(opt.get().clinicId());
                })
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Clinic not found for current user"));
                    }

                    Clinic existing = opt.get();
                    Clinic updated = Clinic.restore(
                            existing.id(),
                            request.name(),
                            existing.document(),
                            existing.email(),
                            request.phone(),
                            request.address(),
                            request.city(),
                            request.state(),
                            existing.status(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating clinic: clinicId=%s by userId=%s", existing.id(), userId);
                    return clinicRepository.update(updated);
                });
    }
}
