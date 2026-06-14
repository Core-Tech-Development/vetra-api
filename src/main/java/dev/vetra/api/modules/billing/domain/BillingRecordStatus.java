package dev.vetra.api.modules.billing.domain;

public enum BillingRecordStatus {
    PENDING_PAYMENT_CREATION,
    PAYMENT_CREATED,
    PAYMENT_CONFIRMED,
    PAYMENT_RECEIVED,
    PAYMENT_OVERDUE,
    PAYMENT_REFUNDED,
    FAILED
}
