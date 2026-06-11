package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.dto.ExamRequestMapper;
import dev.vetra.api.modules.exam.dto.ExamRequestResponse;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists all exam requests for a given patient.
 */
@ApplicationScoped
public class ListExamRequestsByPatientUseCase {

    private static final Logger LOG = Logger.getLogger(ListExamRequestsByPatientUseCase.class);

    private final ExamRequestRepository examRequestRepository;

    @Inject
    public ListExamRequestsByPatientUseCase(ExamRequestRepository examRequestRepository) {
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<List<ExamRequestResponse>> execute(UUID patientId) {
        LOG.debugf("Listing exam requests by patient: patientId=%s", patientId);
        return examRequestRepository.findByPatientId(patientId)
                .map(requests -> requests.stream()
                        .map(ExamRequestMapper::toResponse)
                        .toList());
    }
}
