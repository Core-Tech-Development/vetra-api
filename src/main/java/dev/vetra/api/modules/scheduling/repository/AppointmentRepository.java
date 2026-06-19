package dev.vetra.api.modules.scheduling.repository;

import dev.vetra.api.modules.scheduling.domain.Appointment;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
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
public class AppointmentRepository {

    private static final String SELECT_COLUMNS = """
            id, exam_request_id, specialist_id, availability_slot_id,
            scheduled_start_at, scheduled_end_at, actual_start_at, actual_end_at,
            status, cancel_reason, notes, created_at, updated_at
            """;

    private final PgPool client;

    @Inject
    public AppointmentRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Appointment> save(Appointment appointment) {
        Tuple params = Tuple.tuple();
        params.addUUID(appointment.id());
        params.addUUID(appointment.examRequestId());
        params.addUUID(appointment.specialistId());
        addNullableUUID(params, appointment.availabilitySlotId());
        addNullableOffsetDateTime(params, appointment.scheduledStartAt());
        addNullableOffsetDateTime(params, appointment.scheduledEndAt());
        addNullableOffsetDateTime(params, appointment.actualStartAt());
        addNullableOffsetDateTime(params, appointment.actualEndAt());
        params.addString(appointment.status().name());
        addNullableString(params, appointment.cancelReason());
        addNullableString(params, appointment.notes());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(appointment.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(appointment.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO appointment (id, exam_request_id, specialist_id, availability_slot_id,
                            scheduled_start_at, scheduled_end_at, actual_start_at, actual_end_at,
                            status, cancel_reason, notes, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
                        RETURNING\s""" + SELECT_COLUMNS)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Appointment>> findById(UUID id) {
        return client.preparedQuery("SELECT " + SELECT_COLUMNS + " FROM appointment WHERE id = $1")
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<Appointment>> findByExamRequestId(UUID examRequestId) {
        return client.preparedQuery("SELECT " + SELECT_COLUMNS + " FROM appointment WHERE exam_request_id = $1")
                .execute(Tuple.of(examRequestId))
                .map(this::mapOptional);
    }

    public Uni<Optional<Appointment>> findActiveByExamRequestId(UUID examRequestId) {
        return client.preparedQuery(
                        "SELECT " + SELECT_COLUMNS +
                                " FROM appointment WHERE exam_request_id = $1 AND status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') LIMIT 1")
                .execute(Tuple.of(examRequestId))
                .map(this::mapOptional);
    }

    public Uni<List<Appointment>> findBySpecialistId(UUID specialistId, int offset, int limit) {
        return client.preparedQuery(
                        "SELECT " + SELECT_COLUMNS +
                                " FROM appointment WHERE specialist_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3")
                .execute(Tuple.of(specialistId, limit, offset))
                .map(this::mapList);
    }

    public Uni<List<Appointment>> findAll(int offset, int limit) {
        return client.preparedQuery(
                        "SELECT " + SELECT_COLUMNS +
                                " FROM appointment ORDER BY created_at DESC LIMIT $1 OFFSET $2")
                .execute(Tuple.of(limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countAll() {
        return client.preparedQuery("SELECT count(*) AS total FROM appointment")
                .execute(Tuple.tuple())
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<List<Appointment>> findByStatus(String status, int offset, int limit) {
        return client.preparedQuery(
                        "SELECT " + SELECT_COLUMNS +
                                " FROM appointment WHERE status = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3")
                .execute(Tuple.of(status, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countBySpecialistId(UUID specialistId) {
        return client.preparedQuery("SELECT count(*) AS total FROM appointment WHERE specialist_id = $1")
                .execute(Tuple.of(specialistId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> countByStatus(String status) {
        return client.preparedQuery("SELECT count(*) AS total FROM appointment WHERE status = $1")
                .execute(Tuple.of(status))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<List<Appointment>> findBySpecialistIdAndStatus(UUID specialistId, String status, int offset, int limit) {
        return client.preparedQuery(
                        "SELECT " + SELECT_COLUMNS +
                                " FROM appointment WHERE specialist_id = $1 AND status = $2 ORDER BY created_at DESC LIMIT $3 OFFSET $4")
                .execute(Tuple.of(specialistId, status, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countBySpecialistIdAndStatus(UUID specialistId, String status) {
        return client.preparedQuery("SELECT count(*) AS total FROM appointment WHERE specialist_id = $1 AND status = $2")
                .execute(Tuple.of(specialistId, status))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Appointment> update(Appointment appointment) {
        Tuple params = Tuple.tuple();
        params.addUUID(appointment.id());
        params.addUUID(appointment.examRequestId());
        params.addUUID(appointment.specialistId());
        addNullableUUID(params, appointment.availabilitySlotId());
        addNullableOffsetDateTime(params, appointment.scheduledStartAt());
        addNullableOffsetDateTime(params, appointment.scheduledEndAt());
        addNullableOffsetDateTime(params, appointment.actualStartAt());
        addNullableOffsetDateTime(params, appointment.actualEndAt());
        params.addString(appointment.status().name());
        addNullableString(params, appointment.cancelReason());
        addNullableString(params, appointment.notes());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(appointment.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE appointment
                        SET exam_request_id = $2, specialist_id = $3, availability_slot_id = $4,
                            scheduled_start_at = $5, scheduled_end_at = $6,
                            actual_start_at = $7, actual_end_at = $8,
                            status = $9, cancel_reason = $10, notes = $11, updated_at = $12
                        WHERE id = $1
                        RETURNING\s""" + SELECT_COLUMNS)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Void> updateStatus(UUID id, AppointmentStatus status) {
        Tuple params = Tuple.tuple();
        params.addString(status.name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        params.addUUID(id);

        return client.preparedQuery("""
                        UPDATE appointment
                        SET status = $1, updated_at = $2
                        WHERE id = $3
                        """)
                .execute(params)
                .replaceWithVoid();
    }

    public Uni<Boolean> deleteById(UUID id) {
        return client.preparedQuery("DELETE FROM appointment WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    public Uni<Long> countActiveBySpecialistId(UUID specialistId) {
        return client.preparedQuery("SELECT COUNT(*) AS total FROM appointment WHERE specialist_id = $1 AND status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')")
                .execute(Tuple.of(specialistId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> countByExamRequestId(UUID examRequestId) {
        return client.preparedQuery("SELECT COUNT(*) AS total FROM appointment WHERE exam_request_id = $1")
                .execute(Tuple.of(examRequestId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    // ---- Row Mapping ----

    private Appointment mapRow(Row row) {
        return Appointment.restore(
                row.getUUID("id"),
                row.getUUID("exam_request_id"),
                row.getUUID("specialist_id"),
                getNullableUUID(row, "availability_slot_id"),
                getNullableInstant(row, "scheduled_start_at"),
                getNullableInstant(row, "scheduled_end_at"),
                getNullableInstant(row, "actual_start_at"),
                getNullableInstant(row, "actual_end_at"),
                AppointmentStatus.valueOf(row.getString("status")),
                row.getString("cancel_reason"),
                row.getString("notes"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Appointment> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Appointment> mapList(RowSet<Row> rows) {
        List<Appointment> appointments = new ArrayList<>();
        for (Row row : rows) {
            appointments.add(mapRow(row));
        }
        return appointments;
    }

    // ---- Null-safe helpers ----

    private void addNullableUUID(Tuple params, UUID value) {
        if (value != null) {
            params.addUUID(value);
        } else {
            params.addValue(null);
        }
    }

    private void addNullableOffsetDateTime(Tuple params, Instant value) {
        if (value != null) {
            params.addOffsetDateTime(OffsetDateTime.ofInstant(value, ZoneOffset.UTC));
        } else {
            params.addValue(null);
        }
    }

    private void addNullableString(Tuple params, String value) {
        if (value != null) {
            params.addString(value);
        } else {
            params.addValue(null);
        }
    }

    private UUID getNullableUUID(Row row, String column) {
        try {
            return row.getUUID(column);
        } catch (Exception e) {
            return null;
        }
    }

    private Instant getNullableInstant(Row row, String column) {
        OffsetDateTime odt = row.getOffsetDateTime(column);
        return odt != null ? odt.toInstant() : null;
    }
}
