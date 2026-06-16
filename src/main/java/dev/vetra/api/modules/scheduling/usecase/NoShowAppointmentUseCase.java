package dev.vetra.api.modules.scheduling.usecase;

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
 * Marks an appointment as NO_SHOW.
 * Valid source statuses: ACCEPTED, IN_TRANSIT.
 * If the appointment has a reserved slot, frees it back to AVAILABLE.
 */
@ApplicationScoped
public class NoShowAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(NoShowAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;

    @Inject
    public NoShowAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    AvailabilitySlotRepository slotRepository,
                                    ExamRequestRepository examRequestRepository,
                                    NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(UUID id) {
        return appointmentRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Appointment", id)))
                .flatMap(appointment -> {
                    if (!appointment.status().canTransitionTo(AppointmentStatus.NO_SHOW)) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Cannot mark appointment as no-show with status: " + appointment.status())
                        );
                    }
                    LOG.infof("Marking appointment as no-show: id=%s", id);
                    Appointment updated = appointment.withStatus(AppointmentStatus.NO_SHOW);

                    Uni<Void> freeSlotUni = freeSlotIfReserved(appointment);
                    return freeSlotUni.flatMap(ignored -> appointmentRepository.update(updated))
                            .call(saved -> examRequestRepository.findById(saved.examRequestId())
                                    .flatMap(erOpt -> {
                                        if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                        Uni<Void> notifyClinic = notificationService.notifyClinicAdmins(
                                                erOpt.get().clinicId(),
                                                NotificationType.APPOINTMENT_NO_SHOW,
                                                "Paciente não compareceu",
                                                null, saved.id(), "APPOINTMENT");
                                        Uni<Void> notifySpec = notificationService.notifySpecialist(
                                                saved.specialistId(),
                                                NotificationType.APPOINTMENT_NO_SHOW,
                                                "Paciente não compareceu",
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
