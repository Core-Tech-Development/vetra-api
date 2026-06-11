package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.dto.SlotMapper;
import dev.vetra.api.modules.scheduling.dto.SlotResponse;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Lists availability slots for a specialist with pagination.
 */
@ApplicationScoped
public class ListSlotsUseCase {

    private static final Logger LOG = Logger.getLogger(ListSlotsUseCase.class);

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public ListSlotsUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<PageResponse<SlotResponse>> execute(UUID specialistId, PageRequest pageRequest) {
        LOG.debugf("Listing availability slots: specialistId=%s, page=%d, size=%d", specialistId, pageRequest.page(), pageRequest.size());
        Uni<List<AvailabilitySlot>> slotsUni = slotRepository.findBySpecialistId(
                specialistId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = slotRepository.countBySpecialistId(specialistId);

        return Uni.combine().all().unis(slotsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<SlotResponse> content = tuple.getItem1().stream()
                            .map(SlotMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
