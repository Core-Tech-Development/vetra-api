package dev.vetra.api.modules.tutor.usecase;

import dev.vetra.api.modules.tutor.domain.Tutor;
import dev.vetra.api.modules.tutor.dto.TutorMapper;
import dev.vetra.api.modules.tutor.dto.TutorResponse;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists tutors of a clinic with pagination.
 */
@ApplicationScoped
public class ListTutorsByClinicUseCase {

    private static final Logger LOG = Logger.getLogger(ListTutorsByClinicUseCase.class);

    private final TutorRepository tutorRepository;

    @Inject
    public ListTutorsByClinicUseCase(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public Uni<PageResponse<TutorResponse>> execute(UUID clinicId, PageRequest pageRequest) {
        LOG.debugf("Listing tutors by clinic: clinicId=%s, page=%d, size=%d", clinicId, pageRequest.page(), pageRequest.size());
        Uni<List<Tutor>> tutorsUni = tutorRepository.findByClinicId(clinicId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = tutorRepository.countByClinicId(clinicId);

        return Uni.combine().all().unis(tutorsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<TutorResponse> content = tuple.getItem1().stream()
                            .map(TutorMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
