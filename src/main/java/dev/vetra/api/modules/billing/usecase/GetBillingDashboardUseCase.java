package dev.vetra.api.modules.billing.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.modules.billing.domain.BillingRecordStatus;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetBillingDashboardUseCase {

    private static final TypeReference<DashboardResult> DASHBOARD_REF = new TypeReference<>() {};

    private final BillingRecordRepository repository;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @Inject
    public GetBillingDashboardUseCase(BillingRecordRepository repository,
                                       ReactiveCacheService cache,
                                       CacheTtl ttl) {
        this.repository = repository;
        this.cache = cache;
        this.ttl = ttl;
    }

    public Uni<DashboardResult> execute() {
        return cache.getOrLoad(
                CacheKeys.billingDashboard(),
                ttl.dashboard(),
                DASHBOARD_REF,
                loadFromDatabase());
    }

    private Uni<DashboardResult> loadFromDatabase() {
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
