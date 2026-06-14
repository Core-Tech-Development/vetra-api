package dev.vetra.api.modules.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record AsaasCustomer(
        UUID id,
        UUID clinicId,
        String asaasCustomerId,
        Instant createdAt
) {
    public static AsaasCustomer create(UUID clinicId, String asaasCustomerId) {
        return new AsaasCustomer(UUID.randomUUID(), clinicId, asaasCustomerId, Instant.now());
    }

    public static AsaasCustomer restore(UUID id, UUID clinicId, String asaasCustomerId, Instant createdAt) {
        return new AsaasCustomer(id, clinicId, asaasCustomerId, createdAt);
    }
}
