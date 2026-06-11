package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a clinic staff member by its unique identifier.
 */
@ApplicationScoped
public class GetClinicStaffUseCase {

    private static final Logger LOG = Logger.getLogger(GetClinicStaffUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public GetClinicStaffUseCase(ClinicStaffRepository clinicStaffRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<ClinicStaff> execute(UUID id) {
        LOG.debugf("Fetching clinic staff: id=%s", id);
        return clinicStaffRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("ClinicStaff", id)));
    }
}
