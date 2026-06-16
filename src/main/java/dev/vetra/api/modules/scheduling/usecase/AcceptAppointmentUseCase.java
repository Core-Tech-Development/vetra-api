package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
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

import java.util.Set;
import java.util.UUID;

/**
 * Accepts an appointment. Transitions from WAITING_SPECIALIST_ACCEPTANCE to ACCEPTED.
 * Updates exam request status to SPECIALIST_ASSIGNED.
 */
@ApplicationScoped
public class AcceptAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(AcceptAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final ExamRequestRepository examRequestRepository;
    private final AppointmentOwnershipValidator ownershipValidator;
    private final NotificationService notificationService;

    @Inject
    public AcceptAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    ExamRequestRepository examRequestRepository,
                                    AppointmentOwnershipValidator ownershipValidator,
                                    NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.examRequestRepository = examRequestRepository;
        this.ownershipValidator = ownershipValidator;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(UUID id, String callerUserId, Set<String> callerRoles) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> ownershipValidator.validate(appointment, callerUserId, callerRoles)
                        .replaceWith(appointment))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.ACCEPTED)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot accept appointment with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Accepting appointment: id=%s", id);
                    Appointment updated = appointment.withStatus(AppointmentStatus.ACCEPTED);
                    return appointmentRepository.update(updated)
                            .flatMap(saved -> examRequestRepository.updateStatus(
                                            saved.examRequestId(), ExamRequestStatus.SPECIALIST_ASSIGNED)
                                    .replaceWith(saved))
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        return notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.APPOINTMENT_ACCEPTED,
                                                "Agendamento aceito pelo especialista",
                                                null, saved.id(), "APPOINTMENT");
                                    }));
                });
    }
}
