package dev.vetra.api.modules.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateExamTypePricingRequest(
        @NotNull @Positive Long priceCents,
        @NotNull BigDecimal platformFeePercent,
        @NotNull Boolean active
) {}
