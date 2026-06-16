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

import java.util.UUID;

/**
 * Cancels an appointment with a reason.
 * If the appointment has a reserved slot, frees it back to AVAILABLE.
 * If cancelled from WAITING_SPECIALIST_ACCEPTANCE, reverts exam request to CREATED.
 */
@ApplicationScoped
public class CancelAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(CancelAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;

    @Inject
    public CancelAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    AvailabilitySlotRepository slotRepository,
                                    ExamRequestRepository examRequestRepository,
                                    NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(UUID id, String reason) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.CANCELLED)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot cancel appointment with status: " + appointment.status())
                        );
                    }

                    LOG.infof("Cancelling appointment: id=%s, reason=%s", id, reason);
                    Appointment cancelled = appointment.withCancellation(reason);
                    boolean wasPending = appointment.status() == AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE;

                    Uni<Void> freeSlotUni = freeSlotIfReserved(appointment);
                    return freeSlotUni
                            .flatMap(ignored -> appointmentRepository.update(cancelled))
                            .flatMap(saved -> {
                                if (wasPending) {
                                    LOG.infof("Reverting exam request %s to CREATED after appointment cancellation", saved.examRequestId());
                                    return examRequestRepository.updateStatus(
                                                    saved.examRequestId(), ExamRequestStatus.CREATED)
                                            .replaceWith(saved);
                                }
                                return Uni.createFrom().item(saved);
                            })
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        Uni<Void> notifyClinic = notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.APPOINTMENT_CANCELLED,
                                                "Agendamento cancelado",
                                                null, saved.id(), "APPOINTMENT");
                                        Uni<Void> notifySpec = notificationService.notifySpecialist(
                                                saved.specialistId(),
                                                NotificationType.APPOINTMENT_CANCELLED,
                                                "Agendamento cancelado",
                                                null, saved.id(), "APPOINTMENT");
                                        return Uni.join().all(notifyClinic, notifySpec).andFailFast().replaceWithVoid();
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
