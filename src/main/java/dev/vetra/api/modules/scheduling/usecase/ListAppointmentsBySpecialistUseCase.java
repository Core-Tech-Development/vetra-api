package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.dto.AppointmentMapper;
import dev.vetra.api.modules.scheduling.dto.AppointmentResponse;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists appointments for a specialist with pagination.
 */
@ApplicationScoped
public class ListAppointmentsBySpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(ListAppointmentsBySpecialistUseCase.class);

    private final AppointmentRepository appointmentRepository;

    @Inject
    public ListAppointmentsBySpecialistUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<PageResponse<AppointmentResponse>> execute(UUID specialistId, PageRequest pageRequest) {
        return execute(specialistId, null, pageRequest);
    }

    public Uni<PageResponse<AppointmentResponse>> execute(UUID specialistId, String status, PageRequest pageRequest) {
        LOG.debugf("Listing appointments by specialist: specialistId=%s, status=%s, page=%d, size=%d",
                specialistId, status, pageRequest.page(), pageRequest.size());

        Uni<List<Appointment>> appointmentsUni;
        Uni<Long> countUni;

        if (status != null && !status.isBlank()) {
            appointmentsUni = appointmentRepository.findBySpecialistIdAndStatus(
                    specialistId, status, pageRequest.offset(), pageRequest.size());
            countUni = appointmentRepository.countBySpecialistIdAndStatus(specialistId, status);
        } else {
            appointmentsUni = appointmentRepository.findBySpecialistId(
                    specialistId, pageRequest.offset(), pageRequest.size());
            countUni = appointmentRepository.countBySpecialistId(specialistId);
        }

        return Uni.combine().all().unis(appointmentsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<AppointmentResponse> content = tuple.getItem1().stream()
                            .map(AppointmentMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
