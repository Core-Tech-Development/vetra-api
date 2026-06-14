package dev.vetra.api.modules.billing.dto;

public record BillingDashboardResponse(
        long totalRevenueCents,
        long totalPlatformFeeCents,
        long pendingPayments,
        long overduePayments,
        long totalBillingRecords
) {}
