package dev.vetra.api.modules.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing an audit log entry.
 * Pure Java record — no framework annotations.
 */
public record AuditLog(
        UUID id,
        String actorUserId,
        String entityType,
        UUID entityId,
        String action,
        String previousValue,
        String newValue,
        Instant createdAt
) {

    /**
     * Factory for creating a new audit log entry.
     */
    public static AuditLog create(String actorUserId, String entityType, UUID entityId,
                                   String action, String previousValue, String newValue) {
        return new AuditLog(
                UUID.randomUUID(),
                actorUserId,
                entityType,
                entityId,
                action,
                previousValue,
                newValue,
                Instant.now()
        );
    }

    /**
     * Factory for restoring an audit log entry from persistence.
     */
    public static AuditLog restore(UUID id, String actorUserId, String entityType, UUID entityId,
                                    String action, String previousValue, String newValue,
                                    Instant createdAt) {
        return new AuditLog(id, actorUserId, entityType, entityId, action, previousValue,
                newValue, createdAt);
    }
}
