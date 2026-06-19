package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
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

import java.util.Set;
import java.util.UUID;

/**
 * Marks an appointment as IN_TRANSIT.
 * Valid source status: ACCEPTED.
 */
@ApplicationScoped
public class StartTransitUseCase {

    private static final Logger LOG = Logger.getLogger(StartTransitUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentOwnershipValidator ownershipValidator;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;
    private final LogAuditEventUseCase auditUseCase;

    @Inject
    public StartTransitUseCase(AppointmentRepository appointmentRepository,
                               AppointmentOwnershipValidator ownershipValidator,
                               ExamRequestRepository examRequestRepository,
                               NotificationService notificationService,
                               LogAuditEventUseCase auditUseCase) {
        this.appointmentRepository = appointmentRepository;
        this.ownershipValidator = ownershipValidator;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
        this.auditUseCase = auditUseCase;
    }

    public Uni<Appointment> execute(UUID id, String callerUserId, Set<String> callerRoles) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> ownershipValidator.validate(appointment, callerUserId, callerRoles)
                        .replaceWith(appointment))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.IN_TRANSIT)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot start transit for appointment with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Starting transit for appointment: id=%s", id);
                    Appointment updated = appointment.withStatus(AppointmentStatus.IN_TRANSIT);
                    return appointmentRepository.update(updated)
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        return notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.SPECIALIST_IN_TRANSIT,
                                                "Especialista a caminho",
                                                null, saved.id(), "APPOINTMENT");
                                    }))
                            .flatMap(saved -> auditUseCase.execute(
                                    callerUserId, "APPOINTMENT", saved.id(), "START_TRANSIT", null, saved.toString())
                                    .onFailure().recoverWithNull()
                                    .replaceWith(saved));
                });
    }
}
