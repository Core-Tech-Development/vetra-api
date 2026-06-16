package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Retrieves the clinic profile for the currently authenticated clinic user.
 * Resolves: userId (JWT sub) -> staff record -> clinicId -> clinic.
 */
@ApplicationScoped
public class GetMyClinicProfileUseCase {

    private static final Logger LOG = Logger.getLogger(GetMyClinicProfileUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;
    private final ClinicRepository clinicRepository;

    @Inject
    public GetMyClinicProfileUseCase(ClinicStaffRepository clinicStaffRepository,
                                     ClinicRepository clinicRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
        this.clinicRepository = clinicRepository;
    }

    public Uni<Clinic> execute(String userId) {
        LOG.infof("Fetching clinic profile for userId=%s", userId);
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
                    return Uni.createFrom().item(opt.get());
                });
    }
}
