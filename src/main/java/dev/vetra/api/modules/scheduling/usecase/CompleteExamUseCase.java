package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
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
 * Marks an appointment as WAITING_REPORT and sets the actual end time.
 * Valid source status: IN_SERVICE.
 */
@ApplicationScoped
public class CompleteExamUseCase {

    private static final Logger LOG = Logger.getLogger(CompleteExamUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentOwnershipValidator ownershipValidator;
    private final NotificationService notificationService;
    private final LogAuditEventUseCase auditUseCase;

    @Inject
    public CompleteExamUseCase(AppointmentRepository appointmentRepository,
                               AppointmentOwnershipValidator ownershipValidator,
                               NotificationService notificationService,
                               LogAuditEventUseCase auditUseCase) {
        this.appointmentRepository = appointmentRepository;
        this.ownershipValidator = ownershipValidator;
        this.notificationService = notificationService;
        this.auditUseCase = auditUseCase;
    }

    public Uni<Appointment> execute(UUID id, String callerUserId, Set<String> callerRoles) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> ownershipValidator.validate(appointment, callerUserId, callerRoles)
                        .replaceWith(appointment))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.WAITING_REPORT)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot complete exam for appointment with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Exam completed, appointment awaiting report: id=%s", id);
                    Appointment updated = appointment.withActualEnd(Instant.now(), AppointmentStatus.WAITING_REPORT);
                    return appointmentRepository.update(updated)
                            .call(saved -> notificationService.notifySpecialist(
                                    saved.specialistId(),
                                    NotificationType.EXAM_COMPLETED,
                                    "Exame concluído — emita o laudo",
                                    null, saved.id(), "APPOINTMENT"))
                            .flatMap(saved -> auditUseCase.execute(
                                    callerUserId, "APPOINTMENT", saved.id(), "COMPLETE_EXAM", null, saved.toString())
                                    .onFailure().recoverWithNull()
                                    .replaceWith(saved));
                });
    }
}
