package dev.vetra.api.modules.billing.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillingPaymentResponse(
        UUID id,
        UUID billingRecordId,
        String asaasPaymentId,
        String status,
        String billingType,
        String pixQrCode,
        String pixCopyPaste,
        String boletoUrl,
        String invoiceUrl,
        LocalDate dueDate,
        Instant paidAt,
        long valueCents,
        Long netValueCents,
        Instant createdAt,
        Instant updatedAt
) {}
