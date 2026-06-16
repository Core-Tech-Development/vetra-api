package dev.vetra.api.modules.billing.service;

import dev.vetra.api.shared.exception.BusinessException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@ApplicationScoped
public class BillingPriceResolver {

    private static final Logger LOG = Logger.getLogger(BillingPriceResolver.class);

    private final CachedPricingService cachedPricingService;

    @Inject
    public BillingPriceResolver(CachedPricingService cachedPricingService) {
        this.cachedPricingService = cachedPricingService;
    }

    public Uni<PriceResult> resolvePrice(UUID specialistId, String examType) {
        return cachedPricingService.findBySpecialistIdAndExamType(specialistId, examType)
                .flatMap(specialistOpt -> {
                    if (specialistOpt.isPresent()) {
                        var sp = specialistOpt.get();
                        return cachedPricingService.findByExamType(examType)
                                .map(examOpt -> {
                                    BigDecimal feePercent = examOpt.map(e -> e.platformFeePercent())
                                            .orElse(BigDecimal.valueOf(12));
                                    long platformFeeCents = calculateFee(sp.priceCents(), feePercent);
                                    return new PriceResult(sp.priceCents(), platformFeeCents, feePercent);
                                });
                    }
                    return cachedPricingService.findByExamType(examType)
                            .map(examOpt -> {
                                if (examOpt.isEmpty()) {
                                    throw new BusinessException("NO_PRICING_CONFIGURED",
                                            "No pricing configured for exam type: " + examType);
                                }
                                var pricing = examOpt.get();
                                long platformFeeCents = calculateFee(pricing.priceCents(), pricing.platformFeePercent());
                                return new PriceResult(pricing.priceCents(), platformFeeCents, pricing.platformFeePercent());
                            });
                });
    }

    private long calculateFee(long totalCents, BigDecimal feePercent) {
        return BigDecimal.valueOf(totalCents)
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    public record PriceResult(long totalCents, long platformFeeCents, BigDecimal platformFeePercent) {}
}
