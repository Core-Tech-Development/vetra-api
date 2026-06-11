package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.dto.ExamFileMapper;
import dev.vetra.api.modules.imaging.dto.ExamFileResponse;
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
 * Generates a presigned download URL for an exam file.
 * URL expires after 7200 seconds (2 hours).
 */
@ApplicationScoped
public class GetFileDownloadUrlUseCase {

    private static final Logger LOG = Logger.getLogger(GetFileDownloadUrlUseCase.class);

    private static final int EXPIRY_SECONDS = 7200;

    private final ExamFileRepository examFileRepository;
    private final MinioStorageService storageService;

    @Inject
    public GetFileDownloadUrlUseCase(ExamFileRepository examFileRepository,
                                     MinioStorageService storageService) {
        this.examFileRepository = examFileRepository;
        this.storageService = storageService;
    }

    public Uni<ExamFileResponse> execute(UUID fileId) {
        LOG.debugf("Generating download URL: fileId=%s", fileId);
        return examFileRepository.findById(fileId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ExamFile", fileId));
                    }
                    ExamFile file = opt.get();
                    return Uni.createFrom().item(() -> {
                                try {
                                    return storageService.getPresignedUrl(file.storageKey(), EXPIRY_SECONDS);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to generate presigned URL", e);
                                }
                            }).emitOn(Infrastructure.getDefaultWorkerPool())
                            .map(url -> ExamFileMapper.toResponseWithUrl(file, url));
                });
    }
}
