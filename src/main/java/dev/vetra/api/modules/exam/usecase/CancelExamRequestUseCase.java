package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

/**
 * Cancels an exam request.
 * Only exam requests in CREATED or PENDING_SPECIALIST status can be cancelled.
 */
@ApplicationScoped
public class CancelExamRequestUseCase {

    private static final Logger LOG = Logger.getLogger(CancelExamRequestUseCase.class);

    private static final Set<ExamRequestStatus> CANCELLABLE_STATUSES = Set.of(
            ExamRequestStatus.CREATED,
            ExamRequestStatus.PENDING_SPECIALIST
    );

    private final ExamRequestRepository examRequestRepository;

    @Inject
    public CancelExamRequestUseCase(ExamRequestRepository examRequestRepository) {
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<ExamRequest> execute(UUID id) {
        return examRequestRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ExamRequest", id));
                    }

                    ExamRequest examRequest = opt.get();

                    if (!CANCELLABLE_STATUSES.contains(examRequest.status())) {
                        return Uni.createFrom().failure(new BusinessException(
                                "INVALID_STATUS",
                                "Exam request can only be cancelled when in CREATED or PENDING_SPECIALIST status. Current status: " + examRequest.status()
                        ));
                    }

                    LOG.infof("Cancelling exam request: id=%s, previousStatus=%s", id, examRequest.status());
                    return examRequestRepository.updateStatus(id, ExamRequestStatus.CANCELLED);
                });
    }
}
