package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentRequest;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Schedules a new appointment. An availability slot is always required.
 * Marks the slot as RESERVED. Uses the slot's times for the appointment schedule.
 * Updates exam request status to PENDING_SPECIALIST.
 * Blocks creation if the exam request already has an active appointment.
 */
@ApplicationScoped
public class ScheduleAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(ScheduleAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;

    @Inject
    public ScheduleAppointmentUseCase(AppointmentRepository appointmentRepository,
                                      AvailabilitySlotRepository slotRepository,
                                      ExamRequestRepository examRequestRepository,
                                      NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
    }

    public Uni<Appointment> execute(CreateAppointmentRequest request) {
        if (request.availabilitySlotId() == null) {
            return Uni.createFrom().failure(
                    new BusinessException("SLOT_REQUIRED",
                            "An availability slot is required to schedule an appointment")
            );
        }

        return appointmentRepository.findActiveByExamRequestId(request.examRequestId())
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(
                                new BusinessException("APPOINTMENT_ALREADY_EXISTS",
                                        "This exam request already has an active appointment. Cancel the existing one first.")
                        );
                    }
                    return reserveSlotAndCreateAppointment(request);
                });
    }

    private Uni<Appointment> reserveSlotAndCreateAppointment(CreateAppointmentRequest request) {
        return slotRepository.findById(request.availabilitySlotId())
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("AvailabilitySlot", request.availabilitySlotId()));
                    }
                    var slot = opt.get();

                    // Atomic compare-and-swap: only one concurrent request can reserve the slot
                    return slotRepository.reserveIfAvailable(slot.id())
                            .flatMap(reserved -> {
                                if (!reserved) {
                                    return Uni.createFrom().failure(
                                            new BusinessException("SLOT_NOT_AVAILABLE",
                                                    "Availability slot is not available. Current status: " + slot.status())
                                    );
                                }
                                Appointment appointment = Appointment.create(
                                        request.examRequestId(),
                                        request.specialistId(),
                                        request.availabilitySlotId(),
                                        slot.startAt(),
                                        slot.endAt()
                                );
                                LOG.infof("Scheduling appointment: id=%s, examRequestId=%s, specialistId=%s, slotId=%s",
                                        appointment.id(), request.examRequestId(), request.specialistId(), slot.id());
                                return appointmentRepository.save(appointment)
                                        .flatMap(saved -> examRequestRepository.updateStatus(
                                                        request.examRequestId(), ExamRequestStatus.PENDING_SPECIALIST)
                                                .replaceWith(saved))
                                        .call(saved -> notificationService.notifySpecialist(
                                                saved.specialistId(),
                                                NotificationType.APPOINTMENT_SCHEDULED,
                                                "Novo agendamento recebido",
                                                null, saved.id(), "APPOINTMENT"));
                            });
                });
    }
}
