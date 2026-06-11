package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves the laudo associated with a given appointment.
 */
@ApplicationScoped
public class GetLaudoByAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(GetLaudoByAppointmentUseCase.class);

    private final LaudoRepository laudoRepository;

    @Inject
    public GetLaudoByAppointmentUseCase(LaudoRepository laudoRepository) {
        this.laudoRepository = laudoRepository;
    }

    public Uni<Laudo> execute(UUID appointmentId) {
        LOG.debugf("Fetching laudo by appointment: appointmentId=%s", appointmentId);
        return laudoRepository.findByAppointmentId(appointmentId)
                .map(opt -> opt.orElseThrow(() ->
                        new NotFoundException("Laudo not found for appointment: " + appointmentId)));
    }
}
