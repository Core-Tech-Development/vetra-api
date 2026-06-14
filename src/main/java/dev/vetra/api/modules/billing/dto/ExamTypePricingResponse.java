package dev.vetra.api.modules.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamTypePricingResponse(
        UUID id,
        String examType,
        long priceCents,
        BigDecimal platformFeePercent,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
