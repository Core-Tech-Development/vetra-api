package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.dto.CreateLaudoRequest;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Creates a new draft laudo for an appointment.
 * Only one laudo per appointment is allowed.
 */
@ApplicationScoped
public class CreateLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(CreateLaudoUseCase.class);

    private final LaudoRepository laudoRepository;
    private final AppointmentRepository appointmentRepository;

    @Inject
    public CreateLaudoUseCase(LaudoRepository laudoRepository,
                              AppointmentRepository appointmentRepository) {
        this.laudoRepository = laudoRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Laudo> execute(UUID appointmentId, CreateLaudoRequest request) {
        return appointmentRepository.findById(appointmentId)
                .flatMap(optAppointment -> {
                    if (optAppointment.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Appointment", appointmentId.toString()));
                    }
                    UUID specialistId = optAppointment.get().specialistId();
                    return createDraft(appointmentId, specialistId, request);
                });
    }

    private Uni<Laudo> createDraft(UUID appointmentId, UUID specialistId, CreateLaudoRequest request) {
        return laudoRepository.findByAppointmentId(appointmentId)
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(
                                new DuplicateException("Laudo", "appointmentId", appointmentId.toString())
                        );
                    }

                    Laudo draft = Laudo.createDraft(appointmentId, specialistId);

                    // Apply optional initial content if provided
                    if (request != null && (request.findings() != null || request.conclusion() != null
                            || request.recommendations() != null)) {
                        draft = Laudo.restore(
                                draft.id(),
                                draft.appointmentId(),
                                draft.specialistId(),
                                draft.status(),
                                request.findings(),
                                request.conclusion(),
                                request.recommendations(),
                                draft.pdfStorageKey(),
                                draft.issuedAt(),
                                draft.createdAt(),
                                draft.updatedAt()
                        );
                    }

                    LOG.infof("Creating draft laudo: id=%s, appointmentId=%s, specialistId=%s",
                            draft.id(), appointmentId, specialistId);
                    return laudoRepository.save(draft);
                });
    }
}
