package dev.vetra.api.modules.tutor.usecase;

import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteTutorUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteTutorUseCase.class);

    private final TutorRepository tutorRepository;
    private final PatientRepository patientRepository;

    @Inject
    public DeleteTutorUseCase(TutorRepository tutorRepository, PatientRepository patientRepository) {
        this.tutorRepository = tutorRepository;
        this.patientRepository = patientRepository;
    }

    public Uni<Void> execute(UUID id) {
        return tutorRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Tutor", id));
                    }
                    return patientRepository.countByTutorId(id);
                })
                .flatMap(patientCount -> {
                    if (patientCount > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("TUTOR_HAS_PATIENTS",
                                        "Cannot delete tutor with " + patientCount + " linked patient(s). Remove all patients first.")
                        );
                    }
                    LOG.infof("Deleting tutor: id=%s", id);
                    return tutorRepository.deleteById(id).replaceWithVoid();
                });
    }
}
