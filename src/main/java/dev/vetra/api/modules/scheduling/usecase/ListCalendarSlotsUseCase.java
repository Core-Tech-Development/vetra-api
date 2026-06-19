package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListCalendarSlotsUseCase {

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public ListCalendarSlotsUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<List<AvailabilitySlot>> execute(UUID specialistId, Instant from, Instant to) {
        return slotRepository.findBySpecialistIdAndDateRange(specialistId, from, to);
    }
}
