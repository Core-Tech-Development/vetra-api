package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteExamRequestUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteExamRequestUseCase.class);

    private final ExamRequestRepository examRequestRepository;
    private final AppointmentRepository appointmentRepository;

    @Inject
    public DeleteExamRequestUseCase(ExamRequestRepository examRequestRepository, AppointmentRepository appointmentRepository) {
        this.examRequestRepository = examRequestRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Void> execute(UUID id) {
        return examRequestRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ExamRequest", id));
                    }
                    return appointmentRepository.countByExamRequestId(id);
                })
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("EXAM_REQUEST_HAS_APPOINTMENTS",
                                        "Cannot delete exam request with " + count + " appointment(s). Remove all appointments first.")
                        );
                    }
                    LOG.infof("Deleting exam request: id=%s", id);
                    return examRequestRepository.deleteById(id).replaceWithVoid();
                });
    }
}
