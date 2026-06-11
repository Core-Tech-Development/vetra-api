package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.dto.PatientMapper;
import dev.vetra.api.modules.patient.dto.PatientResponse;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists patients of a tutor with pagination.
 */
@ApplicationScoped
public class ListPatientsByTutorUseCase {

    private static final Logger LOG = Logger.getLogger(ListPatientsByTutorUseCase.class);

    private final PatientRepository patientRepository;

    @Inject
    public ListPatientsByTutorUseCase(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Uni<PageResponse<PatientResponse>> execute(UUID tutorId, PageRequest pageRequest) {
        LOG.debugf("Listing patients by tutor: tutorId=%s, page=%d, size=%d", tutorId, pageRequest.page(), pageRequest.size());
        Uni<List<Patient>> patientsUni = patientRepository.findByTutorId(tutorId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = patientRepository.countByTutorId(tutorId);

        return Uni.combine().all().unis(patientsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<PatientResponse> content = tuple.getItem1().stream()
                            .map(PatientMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
