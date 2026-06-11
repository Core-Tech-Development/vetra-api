package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.CoverageArea;
import dev.vetra.api.modules.specialist.repository.CoverageAreaRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Toggles the active status of a coverage area.
 * Validates that the coverage area belongs to the given specialist (ownership check).
 */
@ApplicationScoped
public class ToggleCoverageAreaUseCase {

    private static final Logger LOG = Logger.getLogger(ToggleCoverageAreaUseCase.class);

    private final CoverageAreaRepository coverageAreaRepository;

    @Inject
    public ToggleCoverageAreaUseCase(CoverageAreaRepository coverageAreaRepository) {
        this.coverageAreaRepository = coverageAreaRepository;
    }

    public Uni<CoverageArea> execute(UUID areaId, UUID specialistId) {
        return coverageAreaRepository.findById(areaId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("CoverageArea", areaId));
                    }

                    CoverageArea area = opt.get();
                    if (!area.specialistId().equals(specialistId)) {
                        return Uni.createFrom().failure(
                                new BusinessException("OWNERSHIP_VIOLATION",
                                        "Coverage area does not belong to the current specialist"));
                    }

                    boolean newActive = !area.active();
                    LOG.infof("Toggling coverage area: id=%s, specialistId=%s, active=%s -> %s",
                            areaId, specialistId, area.active(), newActive);
                    return coverageAreaRepository.updateActive(areaId, newActive);
                });
    }
}
