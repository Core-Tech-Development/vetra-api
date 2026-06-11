package dev.vetra.api.modules.audit.usecase;

import dev.vetra.api.modules.audit.domain.AuditLog;
import dev.vetra.api.modules.audit.repository.AuditLogRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Saves an audit log entry for a given action on an entity.
 */
@ApplicationScoped
public class LogAuditEventUseCase {

    private static final Logger LOG = Logger.getLogger(LogAuditEventUseCase.class);

    private final AuditLogRepository auditLogRepository;

    @Inject
    public LogAuditEventUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Uni<AuditLog> execute(String actorUserId, String entityType, UUID entityId,
                                  String action, String previousValue, String newValue) {
        AuditLog auditLog = AuditLog.create(actorUserId, entityType, entityId,
                action, previousValue, newValue);

        LOG.infof("Logging audit event: entity=%s, entityId=%s, action=%s, actor=%s",
                entityType, entityId, action, actorUserId);

        return auditLogRepository.save(auditLog);
    }
}
