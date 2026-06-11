package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.dto.AppointmentMapper;
import dev.vetra.api.modules.scheduling.dto.AppointmentResponse;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Lists appointments by status with pagination.
 */
@ApplicationScoped
public class ListAppointmentsByStatusUseCase {

    private static final Logger LOG = Logger.getLogger(ListAppointmentsByStatusUseCase.class);

    private final AppointmentRepository appointmentRepository;

    @Inject
    public ListAppointmentsByStatusUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Uni<PageResponse<AppointmentResponse>> execute(String status, PageRequest pageRequest) {
        LOG.debugf("Listing appointments by status: status=%s, page=%d, size=%d", status, pageRequest.page(), pageRequest.size());
        Uni<List<Appointment>> appointmentsUni;
        Uni<Long> countUni;

        if (status != null && !status.isBlank()) {
            validateStatus(status);
            appointmentsUni = appointmentRepository.findByStatus(status, pageRequest.offset(), pageRequest.size());
            countUni = appointmentRepository.countByStatus(status);
        } else {
            appointmentsUni = appointmentRepository.findAll(pageRequest.offset(), pageRequest.size());
            countUni = appointmentRepository.countAll();
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

    private void validateStatus(String status) {
        try {
            AppointmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Invalid appointment status: " + status);
        }
    }
}
