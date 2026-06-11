package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.dto.ClinicMapper;
import dev.vetra.api.modules.clinic.dto.ClinicResponse;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Lists clinics with pagination.
 */
@ApplicationScoped
public class ListClinicsUseCase {

    private static final Logger LOG = Logger.getLogger(ListClinicsUseCase.class);

    private final ClinicRepository clinicRepository;

    @Inject
    public ListClinicsUseCase(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public Uni<PageResponse<ClinicResponse>> execute(PageRequest pageRequest) {
        LOG.debugf("Listing clinics: page=%d, size=%d", pageRequest.page(), pageRequest.size());
        Uni<List<Clinic>> clinicsUni = clinicRepository.findAll(pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = clinicRepository.count();

        return Uni.combine().all().unis(clinicsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<ClinicResponse> content = tuple.getItem1().stream()
                            .map(ClinicMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
