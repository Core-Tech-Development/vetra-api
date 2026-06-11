package dev.vetra.api.modules.audit.repository;

import dev.vetra.api.modules.audit.domain.AuditLog;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuditLogRepository {

    private final PgPool client;

    @Inject
    public AuditLogRepository(PgPool client) {
        this.client = client;
    }

    public Uni<AuditLog> save(AuditLog auditLog) {
        Tuple params = Tuple.tuple();
        params.addUUID(auditLog.id());
        params.addString(auditLog.actorUserId());
        params.addString(auditLog.entityType());
        params.addUUID(auditLog.entityId());
        params.addString(auditLog.action());

        if (auditLog.previousValue() != null) {
            params.addString(auditLog.previousValue());
        } else {
            params.addValue(null);
        }

        if (auditLog.newValue() != null) {
            params.addString(auditLog.newValue());
        } else {
            params.addValue(null);
        }

        params.addOffsetDateTime(OffsetDateTime.ofInstant(auditLog.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO audit_log (id, actor_user_id, entity_type, entity_id, action, previous_value, new_value, created_at)
                        VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7::jsonb, $8)
                        RETURNING id, actor_user_id, entity_type, entity_id, action, previous_value, new_value, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<AuditLog>> findByEntityTypeAndEntityId(String entityType, UUID entityId,
                                                            int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, actor_user_id, entity_type, entity_id, action, previous_value, new_value, created_at
                        FROM audit_log
                        WHERE entity_type = $1 AND entity_id = $2
                        ORDER BY created_at DESC
                        LIMIT $3 OFFSET $4
                        """)
                .execute(Tuple.of(entityType, entityId, limit, offset))
                .map(this::mapList);
    }

    public Uni<List<AuditLog>> findAll(int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, actor_user_id, entity_type, entity_id, action, previous_value, new_value, created_at
                        FROM audit_log
                        ORDER BY created_at DESC
                        LIMIT $1 OFFSET $2
                        """)
                .execute(Tuple.of(limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> count() {
        return client.preparedQuery("SELECT count(*) AS total FROM audit_log")
                .execute(Tuple.tuple())
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    // ---- Row Mapping ----

    private AuditLog mapRow(Row row) {
        return AuditLog.restore(
                row.getUUID("id"),
                row.getString("actor_user_id"),
                row.getString("entity_type"),
                row.getUUID("entity_id"),
                row.getString("action"),
                row.getString("previous_value"),
                row.getString("new_value"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private List<AuditLog> mapList(RowSet<Row> rows) {
        List<AuditLog> auditLogs = new ArrayList<>();
        for (Row row : rows) {
            auditLogs.add(mapRow(row));
        }
        return auditLogs;
    }
}
