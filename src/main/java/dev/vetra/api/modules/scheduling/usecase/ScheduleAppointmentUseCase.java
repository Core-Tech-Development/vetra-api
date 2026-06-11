package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.dto.AppointmentMapper;
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
 * Schedules a new appointment. If an availability slot is provided,
 * marks it as RESERVED. Updates exam request status to PENDING_SPECIALIST.
 * Blocks creation if the exam request already has an active appointment.
 */
@ApplicationScoped
public class ScheduleAppointmentUseCase {

    private static final Logger LOG = Logger.getLogger(ScheduleAppointmentUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ExamRequestRepository examRequestRepository;

    @Inject
    public ScheduleAppointmentUseCase(AppointmentRepository appointmentRepository,
                                      AvailabilitySlotRepository slotRepository,
                                      ExamRequestRepository examRequestRepository) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.examRequestRepository = examRequestRepository;
    }

    public Uni<Appointment> execute(CreateAppointmentRequest request) {
        return appointmentRepository.findActiveByExamRequestId(request.examRequestId())
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(
                                new BusinessException("APPOINTMENT_ALREADY_EXISTS",
                                        "This exam request already has an active appointment. Cancel the existing one first.")
                        );
                    }
                    if (request.availabilitySlotId() != null) {
                        return reserveSlotAndCreateAppointment(request);
                    }
                    return createAppointmentWithoutSlot(request);
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
                    if (slot.status() != SlotStatus.AVAILABLE) {
                        return Uni.createFrom().failure(
                                new BusinessException("SLOT_NOT_AVAILABLE",
                                        "Availability slot is not available. Current status: " + slot.status())
                        );
                    }
                    return slotRepository.updateStatus(slot.id(), SlotStatus.RESERVED)
                            .flatMap(ignored -> saveAndUpdateExamRequest(request));
                });
    }

    private Uni<Appointment> createAppointmentWithoutSlot(CreateAppointmentRequest request) {
        return saveAndUpdateExamRequest(request);
    }

    private Uni<Appointment> saveAndUpdateExamRequest(CreateAppointmentRequest request) {
        Appointment appointment = AppointmentMapper.toDomain(request);
        LOG.infof("Scheduling appointment: id=%s, examRequestId=%s, specialistId=%s",
                appointment.id(), request.examRequestId(), request.specialistId());
        return appointmentRepository.save(appointment)
                .flatMap(saved -> examRequestRepository.updateStatus(
                                request.examRequestId(), ExamRequestStatus.PENDING_SPECIALIST)
                        .replaceWith(saved));
    }
}
