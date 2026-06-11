package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues a draft laudo by transitioning status from DRAFT to ISSUED.
 * Sets issuedAt to the current timestamp.
 * Also transitions the linked appointment to REPORT_ISSUED.
 */
@ApplicationScoped
public class IssueLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(IssueLaudoUseCase.class);

    private final LaudoRepository laudoRepository;
    private final AppointmentRepository appointmentRepository;

    @Inject
    public IssueLaudoUseCase(LaudoRepository laudoRepository, AppointmentRepository appointmentRepository) {
        this.laudoRepository = laudoRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Laudo> execute(UUID laudoId) {
        return laudoRepository.findById(laudoId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", laudoId));
                    }

                    Laudo existing = opt.get();

                    if (existing.status() != LaudoStatus.DRAFT) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Laudo can only be issued from DRAFT status. Current status: " + existing.status())
                        );
                    }

                    Instant now = Instant.now();
                    Laudo issued = Laudo.restore(
                            existing.id(),
                            existing.appointmentId(),
                            existing.specialistId(),
                            LaudoStatus.ISSUED,
                            existing.findings(),
                            existing.conclusion(),
                            existing.recommendations(),
                            existing.pdfStorageKey(),
                            now,
                            existing.createdAt(),
                            now
                    );

                    LOG.infof("Issuing laudo: id=%s, appointmentId=%s", laudoId, existing.appointmentId());
                    return laudoRepository.update(issued)
                            .flatMap(savedLaudo ->
                                    appointmentRepository.updateStatus(savedLaudo.appointmentId(), AppointmentStatus.REPORT_ISSUED)
                                            .onItem().invoke(() -> LOG.infof("Appointment %s transitioned to REPORT_ISSUED", savedLaudo.appointmentId()))
                                            .onFailure().invoke(err -> LOG.warnf(err, "Failed to update appointment status for %s", savedLaudo.appointmentId()))
                                            .onFailure().recoverWithNull()
                                            .map(v -> savedLaudo)
                            );
                });
    }
}
