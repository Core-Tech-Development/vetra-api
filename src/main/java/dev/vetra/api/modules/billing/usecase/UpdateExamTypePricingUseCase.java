package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import dev.vetra.api.modules.billing.repository.ExamTypePricingRepository;
import dev.vetra.api.modules.billing.service.CachedPricingService;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class UpdateExamTypePricingUseCase {

    private final ExamTypePricingRepository repository;
    private final CachedPricingService cachedPricingService;

    @Inject
    public UpdateExamTypePricingUseCase(ExamTypePricingRepository repository,
                                         CachedPricingService cachedPricingService) {
        this.repository = repository;
        this.cachedPricingService = cachedPricingService;
    }

    public Uni<ExamTypePricing> execute(UUID id, long priceCents, BigDecimal platformFeePercent, boolean active) {
        if (priceCents <= 0) {
            return Uni.createFrom().failure(new BusinessException("INVALID_PRICE", "Price must be positive"));
        }

        return repository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) return Uni.createFrom().failure(new NotFoundException("ExamTypePricing", id));
                    ExamTypePricing updated = opt.get().withUpdatedPrice(priceCents, platformFeePercent).withActive(active);
                    return repository.update(updated)
                            .call(result -> cachedPricingService.invalidateExamTypePricing());
                });
    }
}
