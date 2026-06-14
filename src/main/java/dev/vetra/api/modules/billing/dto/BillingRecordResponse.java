package dev.vetra.api.modules.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record BillingRecordResponse(
        UUID id,
        UUID laudoId,
        UUID appointmentId,
        UUID clinicId,
        UUID specialistId,
        String examType,
        long totalCents,
        long platformFeeCents,
        long specialistShareCents,
        String status,
        String asaasPaymentId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}
