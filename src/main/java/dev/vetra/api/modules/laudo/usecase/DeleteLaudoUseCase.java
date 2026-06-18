package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteLaudoUseCase.class);

    private final LaudoRepository laudoRepository;
    private final ExamFileRepository examFileRepository;

    @Inject
    public DeleteLaudoUseCase(LaudoRepository laudoRepository, ExamFileRepository examFileRepository) {
        this.laudoRepository = laudoRepository;
        this.examFileRepository = examFileRepository;
    }

    public Uni<Void> execute(UUID id) {
        return laudoRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", id));
                    }
                    return examFileRepository.countByLaudoId(id);
                })
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("LAUDO_HAS_EXAM_FILES",
                                        "Cannot delete laudo with " + count + " exam file(s). Remove all files first.")
                        );
                    }
                    LOG.infof("Deleting laudo: id=%s", id);
                    return laudoRepository.deleteById(id).replaceWithVoid();
                });
    }
}
