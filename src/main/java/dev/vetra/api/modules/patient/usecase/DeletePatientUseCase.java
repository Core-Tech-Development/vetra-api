package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeletePatientUseCase {

    private static final Logger LOG = Logger.getLogger(DeletePatientUseCase.class);

    private final PatientRepository patientRepository;
    private final ExamRequestRepository examRequestRepository;

    @Inject
    public DeletePatientUseCase(PatientRepository patientRepository, ExamRequestRepository examRequestRepository) {
        this.patientRepository = patientRepository;
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<Void> execute(UUID id) {
        return patientRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Patient", id));
                    }
                    return examRequestRepository.findByPatientId(id);
                })
                .flatMap(examRequests -> {
                    if (!examRequests.isEmpty()) {
                        return Uni.createFrom().failure(
                                new BusinessException("PATIENT_HAS_EXAM_REQUESTS",
                                        "Cannot delete patient with " + examRequests.size() + " exam request(s). Remove all exam requests first.")
                        );
                    }
                    LOG.infof("Deleting patient: id=%s", id);
                    return patientRepository.deleteById(id).replaceWithVoid();
                });
    }
}
