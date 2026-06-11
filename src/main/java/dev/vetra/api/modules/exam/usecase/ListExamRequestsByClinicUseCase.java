package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.dto.ExamRequestWithAppointmentResponse;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists exam requests of a clinic with pagination, including linked appointment summary.
 */
@ApplicationScoped
public class ListExamRequestsByClinicUseCase {

    private static final Logger LOG = Logger.getLogger(ListExamRequestsByClinicUseCase.class);

    private final ExamRequestRepository examRequestRepository;

    @Inject
    public ListExamRequestsByClinicUseCase(ExamRequestRepository examRequestRepository) {
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<PageResponse<ExamRequestWithAppointmentResponse>> execute(UUID clinicId, PageRequest pageRequest) {
        LOG.debugf("Listing exam requests by clinic: clinicId=%s, page=%d, size=%d", clinicId, pageRequest.page(), pageRequest.size());
        Uni<List<ExamRequestWithAppointmentResponse>> requestsUni = examRequestRepository.findByClinicIdWithAppointment(
                clinicId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = examRequestRepository.countByClinicId(clinicId);

        return Uni.combine().all().unis(requestsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<ExamRequestWithAppointmentResponse> content = tuple.getItem1();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
