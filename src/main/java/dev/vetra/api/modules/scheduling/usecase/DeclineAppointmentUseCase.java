package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

/**
 * Specialist declines an appointment request.
 * Cancels the appointment and reverts exam request to CREATED
 * so the clinic can assign another specialist.
 */
@ApplicationScoped
public class DeclineAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(DeclineAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ExamRequestRepository examRequestRepository;
    private final AppointmentOwnershipValidator ownershipValidator;
    private final NotificationService notificationService;

    @Inject
    public DeclineAppointmentUseCase(AppointmentRepository appointmentRepository,
                                     AvailabilitySlotRepository slotRepository,
                                     ExamRequestRepository examRequestRepository,
                                     AppointmentOwnershipValidator ownershipValidator,
                                     NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.examRequestRepository = examRequestRepository;
        this.ownershipValidator = ownershipValidator;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(UUID id, String reason, String callerUserId, Set<String> callerRoles) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> ownershipValidator.validate(appointment, callerUserId, callerRoles)
                        .replaceWith(appointment))
                .flatMap(appointment -> {
                    if (appointment.status() != AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Can only decline appointments with status WAITING_SPECIALIST_ACCEPTANCE. Current: " + appointment.status())
                        );
                    }

                    String declineReason = reason != null && !reason.isBlank()
                            ? reason
                            : "Declined by specialist";

                    LOG.infof("Specialist declining appointment: id=%s, reason=%s", id, declineReason);
                    Appointment declined = appointment.withCancellation(declineReason);

                    Uni<Void> freeSlotUni = freeSlotIfReserved(appointment);
                    return freeSlotUni
                            .flatMap(ignored -> appointmentRepository.update(declined))
                            .flatMap(saved -> {
                                LOG.infof("Reverting exam request %s to CREATED after specialist decline", saved.examRequestId());
                                return examRequestRepository.updateStatus(
                                                saved.examRequestId(), ExamRequestStatus.CREATED)
                                        .replaceWith(saved);
                            })
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        return notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.APPOINTMENT_DECLINED,
                                                "Agendamento recusado pelo especialista",
                                                null, saved.id(), "APPOINTMENT");
                                    }));
                });
    }

    private Uni<Void> freeSlotIfReserved(Appointment appointment) {
        if (appointment.availabilitySlotId() == null) {
            return Uni.createFrom().voidItem();
        }
        return slotRepository.findById(appointment.availabilitySlotId())
                .flatMap(opt -> {
                    if (opt.isPresent() && opt.get().status() == SlotStatus.RESERVED) {
                        LOG.infof("Freeing slot: id=%s back to AVAILABLE", appointment.availabilitySlotId());
                        return slotRepository.updateStatus(appointment.availabilitySlotId(), SlotStatus.AVAILABLE);
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
