package dev.vetra.api.modules.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actorUserId,
        String entityType,
        UUID entityId,
        String action,
        String previousValue,
        String newValue,
        Instant createdAt
) {
}
