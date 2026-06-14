package dev.vetra.api.modules.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record BillingRecord(
        UUID id,
        UUID laudoId,
        UUID appointmentId,
        UUID clinicId,
        UUID specialistId,
        String examType,
        long totalCents,
        long platformFeeCents,
        long specialistShareCents,
        BillingRecordStatus status,
        String asaasPaymentId,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static BillingRecord create(UUID laudoId, UUID appointmentId, UUID clinicId,
                                       UUID specialistId, String examType,
                                       long totalCents, long platformFeeCents) {
        Instant now = Instant.now();
        return new BillingRecord(
                UUID.randomUUID(), laudoId, appointmentId, clinicId, specialistId,
                examType, totalCents, platformFeeCents, totalCents - platformFeeCents,
                BillingRecordStatus.PENDING_PAYMENT_CREATION, null, null, now, now
        );
    }

    public static BillingRecord restore(UUID id, UUID laudoId, UUID appointmentId, UUID clinicId,
                                        UUID specialistId, String examType,
                                        long totalCents, long platformFeeCents, long specialistShareCents,
                                        BillingRecordStatus status, String asaasPaymentId, String errorMessage,
                                        Instant createdAt, Instant updatedAt) {
        return new BillingRecord(id, laudoId, appointmentId, clinicId, specialistId,
                examType, totalCents, platformFeeCents, specialistShareCents,
                status, asaasPaymentId, errorMessage, createdAt, updatedAt);
    }

    public BillingRecord withPaymentCreated(String asaasPaymentId) {
        return new BillingRecord(id, laudoId, appointmentId, clinicId, specialistId,
                examType, totalCents, platformFeeCents, specialistShareCents,
                BillingRecordStatus.PAYMENT_CREATED, asaasPaymentId, null,
                createdAt, Instant.now());
    }

    public BillingRecord withStatus(BillingRecordStatus newStatus) {
        return new BillingRecord(id, laudoId, appointmentId, clinicId, specialistId,
                examType, totalCents, platformFeeCents, specialistShareCents,
                newStatus, asaasPaymentId, errorMessage, createdAt, Instant.now());
    }

    public BillingRecord withError(String errorMessage) {
        return new BillingRecord(id, laudoId, appointmentId, clinicId, specialistId,
                examType, totalCents, platformFeeCents, specialistShareCents,
                status, asaasPaymentId, errorMessage, createdAt, Instant.now());
    }
}
