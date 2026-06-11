package dev.vetra.api.modules.notification.repository;

import dev.vetra.api.modules.notification.domain.Notification;
import dev.vetra.api.modules.notification.domain.NotificationChannel;
import dev.vetra.api.modules.notification.domain.NotificationStatus;
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
public class NotificationRepository {

    private final PgPool client;

    @Inject
    public NotificationRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Notification> save(Notification notification) {
        Tuple params = Tuple.tuple();
        params.addUUID(notification.id());
        params.addString(notification.recipientUserId());
        params.addString(notification.channel().name());
        params.addString(notification.type());
        params.addString(notification.status().name());
        params.addString(notification.subject());

        if (notification.payload() != null) {
            params.addString(notification.payload());
        } else {
            params.addValue(null);
        }

        if (notification.sentAt() != null) {
            params.addOffsetDateTime(OffsetDateTime.ofInstant(notification.sentAt(), ZoneOffset.UTC));
        } else {
            params.addValue(null);
        }

        params.addOffsetDateTime(OffsetDateTime.ofInstant(notification.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO notification (id, recipient_user_id, channel, type, status, subject, payload, sent_at, created_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, $9)
                        RETURNING id, recipient_user_id, channel, type, status, subject, payload, sent_at, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<Notification>> findByRecipientUserId(String recipientUserId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, recipient_user_id, channel, type, status, subject, payload, sent_at, created_at
                        FROM notification
                        WHERE recipient_user_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(recipientUserId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countByRecipientUserId(String recipientUserId) {
        return client.preparedQuery("SELECT count(*) AS total FROM notification WHERE recipient_user_id = $1")
                .execute(Tuple.of(recipientUserId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Notification> updateStatus(UUID id, NotificationStatus status) {
        Tuple params = Tuple.tuple();
        params.addString(status.name());

        if (status == NotificationStatus.SENT) {
            params.addOffsetDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            params.addValue(null);
        }

        params.addUUID(id);

        return client.preparedQuery("""
                        UPDATE notification
                        SET status = $1, sent_at = $2
                        WHERE id = $3
                        RETURNING id, recipient_user_id, channel, type, status, subject, payload, sent_at, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    // ---- Row Mapping ----

    private Notification mapRow(Row row) {
        OffsetDateTime sentAtOdt = row.getOffsetDateTime("sent_at");
        return Notification.restore(
                row.getUUID("id"),
                row.getString("recipient_user_id"),
                NotificationChannel.valueOf(row.getString("channel")),
                row.getString("type"),
                NotificationStatus.valueOf(row.getString("status")),
                row.getString("subject"),
                row.getString("payload"),
                sentAtOdt != null ? sentAtOdt.toInstant() : null,
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private List<Notification> mapList(RowSet<Row> rows) {
        List<Notification> notifications = new ArrayList<>();
        for (Row row : rows) {
            notifications.add(mapRow(row));
        }
        return notifications;
    }
}
