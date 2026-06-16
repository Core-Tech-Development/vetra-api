package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.dto.CreateSlotRequest;
import dev.vetra.api.modules.scheduling.dto.SlotMapper;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Creates a new availability slot for a specialist.
 * Validates that endAt is after startAt and checks for overlapping slots.
 */
@ApplicationScoped
public class CreateSlotUseCase {

    private static final Logger LOG = Logger.getLogger(CreateSlotUseCase.class);

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public CreateSlotUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<AvailabilitySlot> execute(UUID specialistId, CreateSlotRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_SLOT_TIMES", "End time must be after start time")
            );
        }

        return slotRepository.hasOverlappingSlot(specialistId, request.startAt(), request.endAt())
                .flatMap(hasOverlap -> {
                    if (hasOverlap) {
                        return Uni.createFrom().failure(
                                new BusinessException("SLOT_OVERLAP", "This time range overlaps with an existing slot")
                        );
                    }
                    AvailabilitySlot slot = SlotMapper.toDomain(specialistId, request);
                    LOG.infof("Creating availability slot: id=%s, specialistId=%s, startAt=%s, endAt=%s",
                            slot.id(), specialistId, request.startAt(), request.endAt());
                    return slotRepository.save(slot);
                });
    }
}
