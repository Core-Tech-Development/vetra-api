package dev.vetra.api.modules.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamTypePricing(
        UUID id,
        String examType,
        long priceCents,
        BigDecimal platformFeePercent,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamTypePricing create(String examType, long priceCents, BigDecimal platformFeePercent) {
        Instant now = Instant.now();
        return new ExamTypePricing(UUID.randomUUID(), examType, priceCents, platformFeePercent, true, now, now);
    }

    public static ExamTypePricing restore(UUID id, String examType, long priceCents,
                                          BigDecimal platformFeePercent, boolean active,
                                          Instant createdAt, Instant updatedAt) {
        return new ExamTypePricing(id, examType, priceCents, platformFeePercent, active, createdAt, updatedAt);
    }

    public ExamTypePricing withUpdatedPrice(long priceCents, BigDecimal platformFeePercent) {
        return new ExamTypePricing(id, examType, priceCents, platformFeePercent, active, createdAt, Instant.now());
    }

    public ExamTypePricing withActive(boolean active) {
        return new ExamTypePricing(id, examType, priceCents, platformFeePercent, active, createdAt, Instant.now());
    }
}
