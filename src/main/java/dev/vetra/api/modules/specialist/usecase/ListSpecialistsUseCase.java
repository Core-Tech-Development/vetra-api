package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.dto.SpecialistMapper;
import dev.vetra.api.modules.specialist.dto.SpecialistResponse;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Lists specialists with pagination.
 */
@ApplicationScoped
public class ListSpecialistsUseCase {

    private static final Logger LOG = Logger.getLogger(ListSpecialistsUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public ListSpecialistsUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<PageResponse<SpecialistResponse>> execute(PageRequest pageRequest) {
        LOG.debugf("Listing specialists: page=%d, size=%d", pageRequest.page(), pageRequest.size());
        Uni<List<Specialist>> specialistsUni = specialistRepository.findAll(pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = specialistRepository.count();

        return Uni.combine().all().unis(specialistsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<SpecialistResponse> content = tuple.getItem1().stream()
                            .map(SpecialistMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
