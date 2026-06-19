package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.imaging.service.MinioStorageService;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Deletes an exam file from both MinIO storage and the database.
 */
@ApplicationScoped
public class DeleteExamFileUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteExamFileUseCase.class);

    private final ExamFileRepository examFileRepository;
    private final MinioStorageService storageService;

    @Inject
    public DeleteExamFileUseCase(ExamFileRepository examFileRepository,
                                 MinioStorageService storageService) {
        this.examFileRepository = examFileRepository;
        this.storageService = storageService;
    }

    public Uni<Void> execute(UUID fileId) {
        return examFileRepository.findById(fileId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ExamFile", fileId));
                    }
                    ExamFile file = opt.get();
                    LOG.infof("Deleting exam file: id=%s, storageKey=%s", fileId, file.storageKey());

                    return Uni.createFrom().item(() -> {
                                try {
                                    storageService.deleteFile(file.storageKey());
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to delete file from storage", e);
                                }
                                return (Void) null;
                            }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                            .flatMap(ignored -> examFileRepository.deleteById(fileId))
                            .replaceWithVoid();
                });
    }
}
