package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves an exam request by its unique identifier.
 */
@ApplicationScoped
public class GetExamRequestUseCase {

    private static final Logger LOG = Logger.getLogger(GetExamRequestUseCase.class);

    private final ExamRequestRepository examRequestRepository;

    @Inject
    public GetExamRequestUseCase(ExamRequestRepository examRequestRepository) {
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<ExamRequest> execute(UUID id) {
        LOG.debugf("Fetching exam request: id=%s", id);
        return examRequestRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("ExamRequest", id)));
    }
}
