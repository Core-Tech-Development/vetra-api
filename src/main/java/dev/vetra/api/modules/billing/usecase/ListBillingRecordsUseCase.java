package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListBillingRecordsUseCase {

    private final BillingRecordRepository repository;

    @Inject
    public ListBillingRecordsUseCase(BillingRecordRepository repository) {
        this.repository = repository;
    }

    public Uni<List<BillingRecord>> execute(int page, int size) {
        return repository.findAll(page * size, size);
    }

    public Uni<Long> count() {
        return repository.countAll();
    }
}
