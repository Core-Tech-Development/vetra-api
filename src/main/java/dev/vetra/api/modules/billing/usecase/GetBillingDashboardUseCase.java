package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingRecordStatus;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetBillingDashboardUseCase {

    private final BillingRecordRepository repository;

    @Inject
    public GetBillingDashboardUseCase(BillingRecordRepository repository) {
        this.repository = repository;
    }

    public Uni<DashboardResult> execute() {
        Uni<Long> totalRevenue = repository.sumTotalCentsByStatus(BillingRecordStatus.PAYMENT_RECEIVED);
        Uni<Long> totalFees = repository.sumPlatformFeeCentsByStatus(BillingRecordStatus.PAYMENT_RECEIVED);
        Uni<Long> pendingCount = repository.countByStatus(BillingRecordStatus.PAYMENT_CREATED);
        Uni<Long> overdueCount = repository.countByStatus(BillingRecordStatus.PAYMENT_OVERDUE);
        Uni<Long> totalRecords = repository.countAll();

        return Uni.combine().all().unis(totalRevenue, totalFees, pendingCount, overdueCount, totalRecords)
                .with((revenue, fees, pending, overdue, total) ->
                        new DashboardResult(revenue, fees, pending, overdue, total));
    }

    public record DashboardResult(
            long totalRevenueCents,
            long totalPlatformFeeCents,
            long pendingPayments,
            long overduePayments,
            long totalBillingRecords
    ) {}
}
