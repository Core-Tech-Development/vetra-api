package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.CoverageArea;
import dev.vetra.api.modules.specialist.repository.CoverageAreaRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists all coverage areas for a given specialist.
 */
@ApplicationScoped
public class ListCoverageAreasUseCase {

    private static final Logger LOG = Logger.getLogger(ListCoverageAreasUseCase.class);

    private final CoverageAreaRepository coverageAreaRepository;

    @Inject
    public ListCoverageAreasUseCase(CoverageAreaRepository coverageAreaRepository) {
        this.coverageAreaRepository = coverageAreaRepository;
    }

    public Uni<List<CoverageArea>> execute(UUID specialistId) {
        LOG.debugf("Listing coverage areas: specialistId=%s", specialistId);
        return coverageAreaRepository.findBySpecialistId(specialistId);
    }
}
