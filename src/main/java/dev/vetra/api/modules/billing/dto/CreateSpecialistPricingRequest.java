package dev.vetra.api.modules.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSpecialistPricingRequest(
        @NotBlank(message = "Exam type is required")
        String examType,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        Long priceCents
) {}
