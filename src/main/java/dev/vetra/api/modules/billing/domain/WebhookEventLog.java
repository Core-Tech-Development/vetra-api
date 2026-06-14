package dev.vetra.api.modules.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record WebhookEventLog(
        UUID id,
        String eventId,
        String eventType,
        String asaasPaymentId,
        String payload,
        boolean processed,
        String errorMessage,
        Instant createdAt
) {
    public static WebhookEventLog create(String eventId, String eventType,
                                         String asaasPaymentId, String payload) {
        return new WebhookEventLog(UUID.randomUUID(), eventId, eventType,
                asaasPaymentId, payload, false, null, Instant.now());
    }

    public static WebhookEventLog restore(UUID id, String eventId, String eventType,
                                          String asaasPaymentId, String payload,
                                          boolean processed, String errorMessage,
                                          Instant createdAt) {
        return new WebhookEventLog(id, eventId, eventType, asaasPaymentId,
                payload, processed, errorMessage, createdAt);
    }
}
