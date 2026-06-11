package dev.vetra.api.modules.audit.usecase;

import dev.vetra.api.modules.audit.domain.AuditLog;
import dev.vetra.api.modules.audit.dto.AuditLogMapper;
import dev.vetra.api.modules.audit.dto.AuditLogResponse;
import dev.vetra.api.modules.audit.repository.AuditLogRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Lists audit logs with pagination.
 */
@ApplicationScoped
public class ListAuditLogsUseCase {

    private static final Logger LOG = Logger.getLogger(ListAuditLogsUseCase.class);

    private final AuditLogRepository auditLogRepository;

    @Inject
    public ListAuditLogsUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Uni<PageResponse<AuditLogResponse>> execute(PageRequest pageRequest) {
        LOG.debugf("Listing audit logs: page=%d, size=%d", pageRequest.page(), pageRequest.size());
        Uni<List<AuditLog>> logsUni = auditLogRepository.findAll(pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = auditLogRepository.count();

        return Uni.combine().all().unis(logsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<AuditLogResponse> content = tuple.getItem1().stream()
                            .map(AuditLogMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
