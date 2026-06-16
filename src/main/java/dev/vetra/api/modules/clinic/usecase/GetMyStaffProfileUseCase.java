package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Retrieves the staff profile for the currently authenticated clinic user.
 */
@ApplicationScoped
public class GetMyStaffProfileUseCase {

    private static final Logger LOG = Logger.getLogger(GetMyStaffProfileUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public GetMyStaffProfileUseCase(ClinicStaffRepository clinicStaffRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<ClinicStaff> execute(String userId) {
        LOG.infof("Fetching staff profile for userId=%s", userId);
        return clinicStaffRepository.findByUserId(userId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Staff profile not found for current user"));
                    }
                    return Uni.createFrom().item(opt.get());
                });
    }
}
