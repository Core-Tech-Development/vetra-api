package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetBillingRecordUseCase {

    private final BillingRecordRepository repository;

    @Inject
    public GetBillingRecordUseCase(BillingRecordRepository repository) {
        this.repository = repository;
    }

    public Uni<BillingRecord> execute(UUID id) {
        return repository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) return Uni.createFrom().failure(new NotFoundException("BillingRecord", id));
                    return Uni.createFrom().item(opt.get());
                });
    }
}
