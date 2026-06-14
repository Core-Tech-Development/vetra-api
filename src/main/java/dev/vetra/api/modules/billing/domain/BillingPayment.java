package dev.vetra.api.modules.billing.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillingPayment(
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
) {
    public static BillingPayment create(UUID billingRecordId, String asaasPaymentId,
                                        String status, String billingType,
                                        String pixQrCode, String pixCopyPaste,
                                        String boletoUrl, String invoiceUrl,
                                        LocalDate dueDate, long valueCents) {
        Instant now = Instant.now();
        return new BillingPayment(UUID.randomUUID(), billingRecordId, asaasPaymentId,
                status, billingType, pixQrCode, pixCopyPaste, boletoUrl, invoiceUrl,
                dueDate, null, valueCents, null, now, now);
    }

    public static BillingPayment restore(UUID id, UUID billingRecordId, String asaasPaymentId,
                                         String status, String billingType,
                                         String pixQrCode, String pixCopyPaste,
                                         String boletoUrl, String invoiceUrl,
                                         LocalDate dueDate, Instant paidAt,
                                         long valueCents, Long netValueCents,
                                         Instant createdAt, Instant updatedAt) {
        return new BillingPayment(id, billingRecordId, asaasPaymentId, status, billingType,
                pixQrCode, pixCopyPaste, boletoUrl, invoiceUrl, dueDate, paidAt,
                valueCents, netValueCents, createdAt, updatedAt);
    }

    public BillingPayment withStatus(String newStatus) {
        return new BillingPayment(id, billingRecordId, asaasPaymentId, newStatus, billingType,
                pixQrCode, pixCopyPaste, boletoUrl, invoiceUrl, dueDate, paidAt,
                valueCents, netValueCents, createdAt, Instant.now());
    }

    public BillingPayment withPaid(Instant paidAt, Long netValueCents) {
        return new BillingPayment(id, billingRecordId, asaasPaymentId, status, billingType,
                pixQrCode, pixCopyPaste, boletoUrl, invoiceUrl, dueDate, paidAt,
                valueCents, netValueCents, createdAt, Instant.now());
    }
}
