package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.dto.UpdateLaudoRequest;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Updates a draft laudo. Only laudos in DRAFT status can be updated.
 */
@ApplicationScoped
public class UpdateLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateLaudoUseCase.class);

    private final LaudoRepository laudoRepository;

    @Inject
    public UpdateLaudoUseCase(LaudoRepository laudoRepository) {
        this.laudoRepository = laudoRepository;
    }

    public Uni<Laudo> execute(UUID laudoId, UpdateLaudoRequest request) {
        return laudoRepository.findById(laudoId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", laudoId));
                    }

                    Laudo existing = opt.get();

                    if (existing.status() != LaudoStatus.DRAFT) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Laudo can only be updated in DRAFT status. Current status: " + existing.status())
                        );
                    }

                    Laudo updated = Laudo.restore(
                            existing.id(),
                            existing.appointmentId(),
                            existing.specialistId(),
                            existing.status(),
                            request.findings(),
                            request.conclusion(),
                            request.recommendations(),
                            existing.pdfStorageKey(),
                            existing.issuedAt(),
                            existing.createdAt(),
                            Instant.now()
                    );

                    LOG.infof("Updating draft laudo: id=%s", laudoId);
                    return laudoRepository.update(updated);
                });
    }
}
