package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists all exam files for a given appointment.
 */
@ApplicationScoped
public class ListExamFilesUseCase {

    private static final Logger LOG = Logger.getLogger(ListExamFilesUseCase.class);

    private final ExamFileRepository examFileRepository;

    @Inject
    public ListExamFilesUseCase(ExamFileRepository examFileRepository) {
        this.examFileRepository = examFileRepository;
    }

    public Uni<List<ExamFile>> execute(UUID appointmentId) {
        LOG.debugf("Listing exam files: appointmentId=%s", appointmentId);
        return examFileRepository.findByAppointmentId(appointmentId);
    }
}
