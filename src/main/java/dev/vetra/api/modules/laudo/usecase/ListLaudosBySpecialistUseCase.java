package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.dto.LaudoMapper;
import dev.vetra.api.modules.laudo.dto.LaudoResponse;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists laudos by specialist with pagination.
 */
@ApplicationScoped
public class ListLaudosBySpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(ListLaudosBySpecialistUseCase.class);

    private final LaudoRepository laudoRepository;

    @Inject
    public ListLaudosBySpecialistUseCase(LaudoRepository laudoRepository) {
        this.laudoRepository = laudoRepository;
    }

    public Uni<PageResponse<LaudoResponse>> execute(UUID specialistId, PageRequest pageRequest) {
        LOG.debugf("Listing laudos by specialist: specialistId=%s, page=%d, size=%d", specialistId, pageRequest.page(), pageRequest.size());
        Uni<List<Laudo>> laudosUni = laudoRepository.findBySpecialistId(
                specialistId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = laudoRepository.countBySpecialistId(specialistId);

        return Uni.combine().all().unis(laudosUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<LaudoResponse> content = tuple.getItem1().stream()
                            .map(LaudoMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
