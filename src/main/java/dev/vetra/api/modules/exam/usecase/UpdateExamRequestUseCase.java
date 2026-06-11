package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.dto.UpdateExamRequestRequest;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Updates an existing exam request's details.
 * Only exam requests in CREATED status can be updated.
 */
@ApplicationScoped
public class UpdateExamRequestUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateExamRequestUseCase.class);

    private final ExamRequestRepository examRequestRepository;

    @Inject
    public UpdateExamRequestUseCase(ExamRequestRepository examRequestRepository) {
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<ExamRequest> execute(UUID id, UpdateExamRequestRequest request) {
        return examRequestRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ExamRequest", id));
                    }

                    ExamRequest existing = opt.get();

                    if (existing.status() != ExamRequestStatus.CREATED) {
                        return Uni.createFrom().failure(new BusinessException(
                                "INVALID_STATUS",
                                "Exam request can only be updated when in CREATED status. Current status: " + existing.status()
                        ));
                    }

                    ExamPriority priority = request.priority() != null && !request.priority().isBlank()
                            ? ExamPriority.valueOf(request.priority())
                            : existing.priority();

                    ExamRequest updated = ExamRequest.restore(
                            existing.id(),
                            existing.clinicId(),
                            existing.patientId(),
                            request.examType(),
                            priority,
                            request.diagnosticHypothesis(),
                            request.clinicalHistory(),
                            request.additionalNotes(),
                            existing.status(),
                            existing.requestedBy(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating exam request: id=%s", updated.id());
                    return examRequestRepository.update(updated);
                });
    }
}
