package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.repository.CoverageAreaRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Removes a coverage area by its unique identifier.
 */
@ApplicationScoped
public class RemoveCoverageAreaUseCase {

    private static final Logger LOG = Logger.getLogger(RemoveCoverageAreaUseCase.class);

    private final CoverageAreaRepository coverageAreaRepository;

    @Inject
    public RemoveCoverageAreaUseCase(CoverageAreaRepository coverageAreaRepository) {
        this.coverageAreaRepository = coverageAreaRepository;
    }

    public Uni<Void> execute(UUID areaId) {
        return coverageAreaRepository.delete(areaId)
                .flatMap(deleted -> {
                    if (!deleted) {
                        return Uni.createFrom().failure(new NotFoundException("CoverageArea", areaId));
                    }
                    LOG.infof("Removed coverage area: id=%s", areaId);
                    return Uni.createFrom().voidItem();
                });
    }
}
