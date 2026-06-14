package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import dev.vetra.api.modules.billing.repository.ExamTypePricingRepository;
import dev.vetra.api.shared.exception.BusinessException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

@ApplicationScoped
public class CreateExamTypePricingUseCase {

    private static final Logger LOG = Logger.getLogger(CreateExamTypePricingUseCase.class);
    private final ExamTypePricingRepository repository;

    @Inject
    public CreateExamTypePricingUseCase(ExamTypePricingRepository repository) {
        this.repository = repository;
    }

    public Uni<ExamTypePricing> execute(String examType, long priceCents, BigDecimal platformFeePercent) {
        if (priceCents <= 0) {
            return Uni.createFrom().failure(new BusinessException("INVALID_PRICE", "Price must be positive"));
        }
        if (platformFeePercent.compareTo(BigDecimal.ZERO) < 0 || platformFeePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            return Uni.createFrom().failure(new BusinessException("INVALID_FEE", "Fee percent must be between 0 and 100"));
        }

        ExamTypePricing pricing = ExamTypePricing.create(examType, priceCents, platformFeePercent);
        LOG.infof("Creating exam type pricing: type=%s, price=%d, fee=%.2f%%", examType, priceCents, platformFeePercent);
        return repository.save(pricing);
    }
}
