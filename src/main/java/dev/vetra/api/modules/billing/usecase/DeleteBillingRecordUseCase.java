package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class DeleteBillingRecordUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteBillingRecordUseCase.class);

    private final BillingRecordRepository billingRecordRepository;

    @Inject
    public DeleteBillingRecordUseCase(BillingRecordRepository billingRecordRepository) {
        this.billingRecordRepository = billingRecordRepository;
    }

    public Uni<Void> execute(UUID id) {
        return billingRecordRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("BillingRecord", id));
                    }
                    LOG.infof("Deleting billing record: id=%s", id);
                    return billingRecordRepository.deleteById(id).replaceWithVoid();
                });
    }
}
