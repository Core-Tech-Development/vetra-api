package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteClinicUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteClinicUseCase.class);

    private final ClinicRepository clinicRepository;
    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public DeleteClinicUseCase(ClinicRepository clinicRepository, ClinicStaffRepository clinicStaffRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<Void> execute(UUID id) {
        return clinicRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Clinic", id));
                    }
                    return clinicStaffRepository.countByClinicId(id);
                })
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("CLINIC_HAS_STAFF",
                                        "Cannot delete clinic with " + count + " staff member(s). Remove all staff first.")
                        );
                    }
                    LOG.infof("Deleting clinic: id=%s", id);
                    return clinicRepository.deleteById(id).replaceWithVoid();
                });
    }
}
