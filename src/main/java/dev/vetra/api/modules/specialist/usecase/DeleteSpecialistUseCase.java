package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteSpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteSpecialistUseCase.class);

    private final SpecialistRepository specialistRepository;
    private final AppointmentRepository appointmentRepository;

    @Inject
    public DeleteSpecialistUseCase(SpecialistRepository specialistRepository, AppointmentRepository appointmentRepository) {
        this.specialistRepository = specialistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<Void> execute(UUID id) {
        return specialistRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Specialist", id));
                    }
                    return appointmentRepository.countActiveBySpecialistId(id);
                })
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(
                                new BusinessException("SPECIALIST_HAS_ACTIVE_APPOINTMENTS",
                                        "Cannot delete specialist with " + count + " active appointment(s). Cancel or complete all appointments first.")
                        );
                    }
                    LOG.infof("Deleting specialist: id=%s", id);
                    return specialistRepository.deleteById(id).replaceWithVoid();
                });
    }
}
