package dev.vetra.api.modules.laudo.repository;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LaudoRepository {

    private final PgPool client;

    @Inject
    public LaudoRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Laudo> save(Laudo laudo) {
        Tuple params = Tuple.tuple();
        params.addUUID(laudo.id());
        params.addUUID(laudo.appointmentId());
        params.addUUID(laudo.specialistId());
        params.addString(laudo.status().name());
        addNullableString(params, laudo.findings());
        addNullableString(params, laudo.conclusion());
        addNullableString(params, laudo.recommendations());
        addNullableString(params, laudo.pdfStorageKey());
        addNullableOffsetDateTime(params, laudo.issuedAt());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(laudo.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(laudo.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO laudo (id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
                        RETURNING id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Laudo>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at
                        FROM laudo
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<Laudo>> findByAppointmentId(UUID appointmentId) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at
                        FROM laudo
                        WHERE appointment_id = $1
                        """)
                .execute(Tuple.of(appointmentId))
                .map(this::mapOptional);
    }

    public Uni<List<Laudo>> findBySpecialistId(UUID specialistId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at
                        FROM laudo
                        WHERE specialist_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(specialistId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countBySpecialistId(UUID specialistId) {
        return client.preparedQuery("SELECT count(*) AS total FROM laudo WHERE specialist_id = $1")
                .execute(Tuple.of(specialistId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Laudo> update(Laudo laudo) {
        Tuple params = Tuple.tuple();
        params.addUUID(laudo.id());
        params.addString(laudo.status().name());
        addNullableString(params, laudo.findings());
        addNullableString(params, laudo.conclusion());
        addNullableString(params, laudo.recommendations());
        addNullableString(params, laudo.pdfStorageKey());
        addNullableOffsetDateTime(params, laudo.issuedAt());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(laudo.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE laudo
                        SET status = $2, findings = $3, conclusion = $4, recommendations = $5,
                            pdf_storage_key = $6, issued_at = $7, updated_at = $8
                        WHERE id = $1
                        RETURNING id, appointment_id, specialist_id, status, findings, conclusion,
                            recommendations, pdf_storage_key, issued_at, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    // ---- Row Mapping ----

    private Laudo mapRow(Row row) {
        OffsetDateTime issuedAtOdt = row.getOffsetDateTime("issued_at");
        Instant issuedAt = issuedAtOdt != null ? issuedAtOdt.toInstant() : null;

        return Laudo.restore(
                row.getUUID("id"),
                row.getUUID("appointment_id"),
                row.getUUID("specialist_id"),
                LaudoStatus.valueOf(row.getString("status")),
                row.getString("findings"),
                row.getString("conclusion"),
                row.getString("recommendations"),
                row.getString("pdf_storage_key"),
                issuedAt,
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Laudo> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Laudo> mapList(RowSet<Row> rows) {
        List<Laudo> laudos = new ArrayList<>();
        for (Row row : rows) {
            laudos.add(mapRow(row));
        }
        return laudos;
    }

    private void addNullableString(Tuple params, String value) {
        if (value != null) {
            params.addString(value);
        } else {
            params.addValue(null);
        }
    }

    private void addNullableOffsetDateTime(Tuple params, Instant instant) {
        if (instant != null) {
            params.addOffsetDateTime(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
        } else {
            params.addOffsetDateTime(null);
        }
    }
}
