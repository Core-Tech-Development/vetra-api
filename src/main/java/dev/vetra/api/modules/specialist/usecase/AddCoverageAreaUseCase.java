package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.CoverageArea;
import dev.vetra.api.modules.specialist.dto.AddCoverageAreaRequest;
import dev.vetra.api.modules.specialist.repository.CoverageAreaRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Adds a coverage area to an existing specialist.
 * Verifies that the specialist exists before creating the coverage area.
 */
@ApplicationScoped
public class AddCoverageAreaUseCase {

    private static final Logger LOG = Logger.getLogger(AddCoverageAreaUseCase.class);

    private final SpecialistRepository specialistRepository;
    private final CoverageAreaRepository coverageAreaRepository;

    @Inject
    public AddCoverageAreaUseCase(SpecialistRepository specialistRepository,
                                  CoverageAreaRepository coverageAreaRepository) {
        this.specialistRepository = specialistRepository;
        this.coverageAreaRepository = coverageAreaRepository;
    }

    public Uni<CoverageArea> execute(UUID specialistId, AddCoverageAreaRequest request) {
        return specialistRepository.findById(specialistId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Specialist", specialistId));
                    }

                    CoverageArea area = CoverageArea.create(
                            specialistId,
                            request.city(),
                            request.state(),
                            request.radiusKm()
                    );

                    LOG.infof("Adding coverage area: specialistId=%s, city=%s, state=%s", specialistId, request.city(), request.state());
                    return coverageAreaRepository.save(area);
                });
    }
}
