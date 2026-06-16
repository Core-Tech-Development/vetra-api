package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.dto.UpdateMyStaffProfileRequest;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Updates the staff profile for the currently authenticated clinic user.
 * Only allows changing name and phone. Role, email, and other fields are preserved.
 */
@ApplicationScoped
public class UpdateMyStaffProfileUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateMyStaffProfileUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public UpdateMyStaffProfileUseCase(ClinicStaffRepository clinicStaffRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<ClinicStaff> execute(String userId, UpdateMyStaffProfileRequest request) {
        LOG.infof("Updating staff profile for userId=%s", userId);
        return clinicStaffRepository.findByUserId(userId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Staff profile not found for current user"));
                    }

                    ClinicStaff existing = opt.get();
                    ClinicStaff updated = ClinicStaff.restore(
                            existing.id(),
                            existing.clinicId(),
                            existing.userId(),
                            request.name(),
                            existing.email(),
                            request.phone(),
                            existing.role(),
                            existing.status(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating staff profile: staffId=%s by userId=%s", existing.id(), userId);
                    return clinicStaffRepository.update(updated);
                });
    }
}
