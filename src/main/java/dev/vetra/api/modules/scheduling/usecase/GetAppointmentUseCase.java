package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves an appointment by its unique identifier.
 */
@ApplicationScoped
public class GetAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(GetAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;

    @Inject
    public GetAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Appointment> execute(UUID id) {
        LOG.debugf("Fetching appointment: id=%s", id);
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)));
    }
}
