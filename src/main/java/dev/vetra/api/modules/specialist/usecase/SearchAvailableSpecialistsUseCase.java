package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.dto.SpecialistMapper;
import dev.vetra.api.modules.specialist.dto.SpecialistResponse;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Searches for available (ACTIVE) specialists by city and state, optionally filtered by specialty.
 * Matches specialists whose base location or coverage areas include the requested city/state.
 */
@ApplicationScoped
public class SearchAvailableSpecialistsUseCase {

    private static final Logger LOG = Logger.getLogger(SearchAvailableSpecialistsUseCase.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public SearchAvailableSpecialistsUseCase(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<List<SpecialistResponse>> execute(String city, String state, String specialty) {
        LOG.infof("Searching available specialists: city=%s, state=%s, specialty=%s", city, state, specialty);

        return specialistRepository.searchAvailable(city, state, specialty)
                .map(specialists -> specialists.stream()
                        .map(SpecialistMapper::toResponse)
                        .toList());
    }
}
