package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Marks an appointment as IN_SERVICE and sets the actual start time.
 * Valid source status: IN_TRANSIT.
 */
@ApplicationScoped
public class StartServiceUseCase {

    private static final Logger LOG = Logger.getLogger(StartServiceUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentOwnershipValidator ownershipValidator;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;

    @Inject
    public StartServiceUseCase(AppointmentRepository appointmentRepository,
                               AppointmentOwnershipValidator ownershipValidator,
                               ExamRequestRepository examRequestRepository,
                               NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.ownershipValidator = ownershipValidator;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(UUID id, String callerUserId, Set<String> callerRoles) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> ownershipValidator.validate(appointment, callerUserId, callerRoles)
                        .replaceWith(appointment))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.IN_SERVICE)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot start service for appointment with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Starting service for appointment: id=%s", id);
                    Appointment updated = appointment.withActualStart(Instant.now(), AppointmentStatus.IN_SERVICE);
                    return appointmentRepository.update(updated)
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        return notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.EXAM_IN_SERVICE,
                                                "Exame em andamento",
                                                null, saved.id(), "APPOINTMENT");
                                    }));
                });
    }
}
