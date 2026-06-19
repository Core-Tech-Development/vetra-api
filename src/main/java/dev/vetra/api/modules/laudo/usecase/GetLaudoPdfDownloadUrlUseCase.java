package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.imaging.service.MinioStorageService;
import dev.vetra.api.modules.laudo.dto.LaudoPdfUrlResponse;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Generates a presigned download URL for a laudo PDF stored in MinIO.
 * URL expires after 7200 seconds (2 hours).
 */
@ApplicationScoped
public class GetLaudoPdfDownloadUrlUseCase {

    private static final Logger LOG = Logger.getLogger(GetLaudoPdfDownloadUrlUseCase.class);
    private static final int EXPIRY_SECONDS = 7200;

    private final LaudoRepository laudoRepository;
    private final MinioStorageService storageService;

    @Inject
    public GetLaudoPdfDownloadUrlUseCase(LaudoRepository laudoRepository,
                                          MinioStorageService storageService) {
        this.laudoRepository = laudoRepository;
        this.storageService = storageService;
    }

    public Uni<LaudoPdfUrlResponse> execute(UUID laudoId) {
        LOG.debugf("Generating PDF download URL: laudoId=%s", laudoId);
        return laudoRepository.findById(laudoId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", laudoId));
                    }
                    var laudo = opt.get();
                    if (laudo.pdfStorageKey() == null) {
                        return Uni.createFrom().failure(
                                new BusinessException("PDF_NOT_GENERATED",
                                        "PDF has not been generated for this laudo yet"));
                    }
                    return Uni.createFrom().item(() -> {
                                try {
                                    return storageService.getPresignedUrl(laudo.pdfStorageKey(), EXPIRY_SECONDS);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to generate presigned URL for laudo PDF", e);
                                }
                            }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                            .map(LaudoPdfUrlResponse::new);
                });
    }
}
