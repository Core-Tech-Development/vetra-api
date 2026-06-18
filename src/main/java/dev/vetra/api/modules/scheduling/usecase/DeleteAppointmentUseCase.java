package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final LaudoRepository laudoRepository;

    @Inject
    public DeleteAppointmentUseCase(AppointmentRepository appointmentRepository, LaudoRepository laudoRepository) {
        this.appointmentRepository = appointmentRepository;
        this.laudoRepository = laudoRepository;
    }

    public Uni<Void> execute(UUID id) {
        return appointmentRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Appointment", id));
                    }
                    return laudoRepository.countByAppointmentId(id);
                })
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("APPOINTMENT_HAS_LAUDOS",
                                        "Cannot delete appointment with " + count + " laudo(s). Remove all laudos first.")
                        );
                    }
                    LOG.infof("Deleting appointment: id=%s", id);
                    return appointmentRepository.deleteById(id).replaceWithVoid();
                });
    }
}
