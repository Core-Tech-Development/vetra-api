package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Blocks an availability slot. Only AVAILABLE slots can be blocked.
 */
@ApplicationScoped
public class BlockSlotUseCase {

    private static final Logger LOG = Logger.getLogger(BlockSlotUseCase.class);

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public BlockSlotUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<AvailabilitySlot> execute(UUID id) {
        return slotRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("AvailabilitySlot", id));
                    }
                    var slot = opt.get();
                    if (slot.status() != SlotStatus.AVAILABLE) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_SLOT_STATUS",
                                        "Only slots with AVAILABLE status can be blocked. Current status: " + slot.status())
                        );
                    }
                    LOG.infof("Blocking availability slot: id=%s", id);
                    return slotRepository.updateStatus(id, SlotStatus.BLOCKED)
                            .flatMap(v -> slotRepository.findById(id))
                            .map(updated -> updated.orElseThrow());
                });
    }
}
