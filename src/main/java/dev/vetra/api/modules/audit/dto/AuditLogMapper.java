package dev.vetra.api.modules.audit.dto;

import dev.vetra.api.modules.audit.domain.AuditLog;

/**
 * Static mapping between AuditLog domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class AuditLogMapper {

    private AuditLogMapper() {
        // utility class
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.id(),
                auditLog.actorUserId(),
                auditLog.entityType(),
                auditLog.entityId(),
                auditLog.action(),
                auditLog.previousValue(),
                auditLog.newValue(),
                auditLog.createdAt()
        );
    }
}
