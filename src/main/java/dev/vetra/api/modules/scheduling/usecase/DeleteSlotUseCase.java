package dev.vetra.api.modules.scheduling.usecase;

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
 * Deletes an availability slot. Only slots with AVAILABLE status can be deleted.
 */
@ApplicationScoped
public class DeleteSlotUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteSlotUseCase.class);

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public DeleteSlotUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<Void> execute(UUID id) {
        return slotRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("AvailabilitySlot", id));
                    }
                    var slot = opt.get();
                    if (slot.status() != SlotStatus.AVAILABLE) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_SLOT_STATUS",
                                        "Only slots with AVAILABLE status can be deleted. Current status: " + slot.status())
                        );
                    }
                    LOG.infof("Deleting availability slot: id=%s", id);
                    return slotRepository.delete(id).replaceWithVoid();
                });
    }
}
