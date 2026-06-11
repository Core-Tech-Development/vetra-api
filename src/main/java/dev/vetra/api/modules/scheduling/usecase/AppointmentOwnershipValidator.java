package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.ForbiddenException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;

/**
 * Validates that the calling user is the specialist assigned to the appointment.
 * Platform administrators bypass ownership checks.
 */
@ApplicationScoped
public class AppointmentOwnershipValidator {

    private static final Logger LOG = Logger.getLogger(AppointmentOwnershipValidator.class);

    private final SpecialistRepository specialistRepository;

    @Inject
    public AppointmentOwnershipValidator(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Uni<Void> validate(Appointment appointment, String callerUserId, Set<String> callerRoles) {
        if (callerRoles != null && callerRoles.contains("PLATFORM_ADMIN")) {
            return Uni.createFrom().voidItem();
        }

        if (callerUserId == null || callerUserId.isBlank()) {
            return Uni.createFrom().failure(
                    new ForbiddenException("UNAUTHENTICATED", "Authentication required for this operation"));
        }

        return specialistRepository.findByUserId(callerUserId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ForbiddenException("NOT_SPECIALIST", "Caller is not registered as a specialist"));
                    }
                    if (!opt.get().id().equals(appointment.specialistId())) {
                        LOG.warnf("Ownership violation: user=%s specialist=%s tried to modify appointment=%s owned by specialist=%s",
                                callerUserId, opt.get().id(), appointment.id(), appointment.specialistId());
                        return Uni.createFrom().failure(
                                new ForbiddenException("NOT_APPOINTMENT_OWNER",
                                        "Only the assigned specialist can perform this action"));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
