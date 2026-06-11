package dev.vetra.api.modules.scheduling.repository;

import dev.vetra.api.modules.scheduling.domain.AppointmentNote;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AppointmentNoteRepository {

    private final PgPool client;

    @Inject
    public AppointmentNoteRepository(PgPool client) {
        this.client = client;
    }

    public Uni<AppointmentNote> save(AppointmentNote note) {
        return client.preparedQuery("""
                        INSERT INTO appointment_note (id, appointment_id, author_user_id, title, content, created_at)
                        VALUES ($1, $2, $3, $4, $5, $6)
                        RETURNING id, appointment_id, author_user_id, title, content, created_at
                        """)
                .execute(Tuple.of(
                        note.id(),
                        note.appointmentId(),
                        note.authorUserId(),
                        note.title(),
                        note.content(),
                        OffsetDateTime.ofInstant(note.createdAt(), ZoneOffset.UTC)
                ))
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<AppointmentNote>> findByAppointmentId(UUID appointmentId) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, author_user_id, title, content, created_at
                        FROM appointment_note
                        WHERE appointment_id = $1
                        ORDER BY created_at ASC
                        """)
                .execute(Tuple.of(appointmentId))
                .map(rows -> {
                    List<AppointmentNote> notes = new ArrayList<>();
                    for (Row row : rows) {
                        notes.add(mapRow(row));
                    }
                    return notes;
                });
    }

    private AppointmentNote mapRow(Row row) {
        return AppointmentNote.restore(
                row.getUUID("id"),
                row.getUUID("appointment_id"),
                row.getString("author_user_id"),
                row.getString("title"),
                row.getString("content"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }
}
