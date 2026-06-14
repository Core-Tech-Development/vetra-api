package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.WebhookEventLog;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WebhookEventLogRepository {

    private final PgPool client;

    @Inject
    public WebhookEventLogRepository(PgPool client) {
        this.client = client;
    }

    public Uni<WebhookEventLog> save(WebhookEventLog event) {
        Tuple params = Tuple.tuple();
        params.addUUID(event.id());
        params.addString(event.eventId());
        params.addString(event.eventType());
        addNullableString(params, event.asaasPaymentId());
        params.addString(event.payload());
        params.addBoolean(event.processed());
        addNullableString(params, event.errorMessage());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO webhook_event_log (id, event_id, event_type, asaas_payment_id,
                            payload, processed, error_message, created_at)
                        VALUES ($1, $2, $3, $4, $5::jsonb, $6, $7, $8)
                        RETURNING id, event_id, event_type, asaas_payment_id, payload::text,
                            processed, error_message, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<WebhookEventLog>> findByEventId(String eventId) {
        return client.preparedQuery("""
                        SELECT id, event_id, event_type, asaas_payment_id, payload::text,
                            processed, error_message, created_at
                        FROM webhook_event_log
                        WHERE event_id = $1
                        """)
                .execute(Tuple.of(eventId))
                .map(this::mapOptional);
    }

    public Uni<Void> markProcessed(UUID id) {
        return client.preparedQuery("""
                        UPDATE webhook_event_log
                        SET processed = true
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .replaceWithVoid();
    }

    public Uni<Void> markError(UUID id, String errorMessage) {
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        addNullableString(params, errorMessage);

        return client.preparedQuery("""
                        UPDATE webhook_event_log
                        SET error_message = $2
                        WHERE id = $1
                        """)
                .execute(params)
                .replaceWithVoid();
    }

    // ---- Row Mapping ----

    private WebhookEventLog mapRow(Row row) {
        return WebhookEventLog.restore(
                row.getUUID("id"),
                row.getString("event_id"),
                row.getString("event_type"),
                row.getString("asaas_payment_id"),
                row.getString("payload"),
                row.getBoolean("processed"),
                row.getString("error_message"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private Optional<WebhookEventLog> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private void addNullableString(Tuple params, String value) {
        if (value != null) {
            params.addString(value);
        } else {
            params.addValue(null);
        }
    }
}
