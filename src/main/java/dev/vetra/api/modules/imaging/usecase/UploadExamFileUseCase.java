package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.imaging.service.MinioStorageService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.UUID;

/**
 * Uploads an exam file to MinIO and saves metadata to the database.
 * MinIO SDK is blocking, so the upload is executed on a worker thread.
 */
@ApplicationScoped
public class UploadExamFileUseCase {

    private static final Logger LOG = Logger.getLogger(UploadExamFileUseCase.class);

    private final ExamFileRepository examFileRepository;
    private final MinioStorageService storageService;

    @Inject
    public UploadExamFileUseCase(ExamFileRepository examFileRepository,
                                 MinioStorageService storageService) {
        this.examFileRepository = examFileRepository;
        this.storageService = storageService;
    }

    public Uni<ExamFile> execute(UUID appointmentId, String fileName, String fileType,
                                 String contentType, InputStream inputStream, long size,
                                 String uploadedBy) {
        return Uni.createFrom().item(() -> {
                    String key = "exams/" + appointmentId + "/" + UUID.randomUUID() + "_" + fileName;
                    try {
                        storageService.uploadFile(key, inputStream, size, contentType);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload file to storage", e);
                    }
                    return key;
                }).emitOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(key -> {
                    ExamFile file = ExamFile.create(appointmentId, fileName, fileType, contentType,
                            key, size, uploadedBy);
                    LOG.infof("Uploading exam file: id=%s, appointmentId=%s, fileName=%s",
                            file.id(), appointmentId, fileName);
                    return examFileRepository.save(file);
                });
    }
}
