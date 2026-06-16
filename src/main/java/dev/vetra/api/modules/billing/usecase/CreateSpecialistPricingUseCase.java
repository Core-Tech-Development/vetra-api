package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.SpecialistPricing;
import dev.vetra.api.modules.billing.repository.SpecialistPricingRepository;
import dev.vetra.api.modules.billing.service.CachedPricingService;
import dev.vetra.api.shared.exception.BusinessException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class CreateSpecialistPricingUseCase {

    private static final Logger LOG = Logger.getLogger(CreateSpecialistPricingUseCase.class);
    private final SpecialistPricingRepository repository;
    private final CachedPricingService cachedPricingService;

    @Inject
    public CreateSpecialistPricingUseCase(SpecialistPricingRepository repository,
                                           CachedPricingService cachedPricingService) {
        this.repository = repository;
        this.cachedPricingService = cachedPricingService;
    }

    public Uni<SpecialistPricing> execute(UUID specialistId, String examType, long priceCents) {
        if (priceCents <= 0) {
            return Uni.createFrom().failure(new BusinessException("INVALID_PRICE", "Price must be positive"));
        }

        SpecialistPricing pricing = SpecialistPricing.create(specialistId, examType, priceCents);
        LOG.infof("Creating specialist pricing: specialist=%s, exam=%s, price=%d", specialistId, examType, priceCents);
        return repository.save(pricing)
                .call(result -> cachedPricingService.invalidateSpecialistPricing(result.specialistId()));
    }
}
