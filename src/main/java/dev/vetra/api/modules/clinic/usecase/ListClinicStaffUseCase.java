package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.dto.ClinicStaffMapper;
import dev.vetra.api.modules.clinic.dto.ClinicStaffResponse;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists clinic staff members of a clinic with pagination.
 */
@ApplicationScoped
public class ListClinicStaffUseCase {

    private static final Logger LOG = Logger.getLogger(ListClinicStaffUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;

    @Inject
    public ListClinicStaffUseCase(ClinicStaffRepository clinicStaffRepository) {
        this.clinicStaffRepository = clinicStaffRepository;
    }

    public Uni<PageResponse<ClinicStaffResponse>> execute(UUID clinicId, PageRequest pageRequest) {
        LOG.debugf("Listing clinic staff by clinic: clinicId=%s, page=%d, size=%d", clinicId, pageRequest.page(), pageRequest.size());
        Uni<List<ClinicStaff>> staffUni = clinicStaffRepository.findByClinicId(clinicId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = clinicStaffRepository.countByClinicId(clinicId);

        return Uni.combine().all().unis(staffUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<ClinicStaffResponse> content = tuple.getItem1().stream()
                            .map(ClinicStaffMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
