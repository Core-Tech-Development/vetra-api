package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Completes an appointment after the report has been issued.
 * Transitions from REPORT_ISSUED to COMPLETED.
 * Triggered by the clinic confirming receipt of the report.
 */
@ApplicationScoped
public class CompleteAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(CompleteAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;

    @Inject
    public CompleteAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Appointment> execute(UUID id) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.COMPLETED)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot complete appointment with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Completing appointment: id=%s", id);
                    Appointment updated = appointment.withStatus(AppointmentStatus.COMPLETED);
                    return appointmentRepository.update(updated);
                });
    }
}
