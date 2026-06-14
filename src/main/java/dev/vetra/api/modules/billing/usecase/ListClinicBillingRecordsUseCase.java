package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListClinicBillingRecordsUseCase {

    private final BillingRecordRepository repository;

    @Inject
    public ListClinicBillingRecordsUseCase(BillingRecordRepository repository) {
        this.repository = repository;
    }

    public Uni<List<BillingRecord>> execute(UUID clinicId, int page, int size) {
        return repository.findByClinicId(clinicId, page * size, size);
    }

    public Uni<Long> count(UUID clinicId) {
        return repository.countByClinicId(clinicId);
    }
}
