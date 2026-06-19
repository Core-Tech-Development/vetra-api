package dev.vetra.api.modules.scheduling.repository;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.domain.SlotStatus;
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
public class AvailabilitySlotRepository {

    private final PgPool client;

    @Inject
    public AvailabilitySlotRepository(PgPool client) {
        this.client = client;
    }

    public Uni<AvailabilitySlot> save(AvailabilitySlot slot) {
        Tuple params = Tuple.tuple();
        params.addUUID(slot.id());
        params.addUUID(slot.specialistId());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.startAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.endAt(), ZoneOffset.UTC));
        params.addString(slot.status().name());
        params.addString(slot.label());
        params.addUUID(slot.recurrenceGroupId());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO availability_slot (id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                        RETURNING id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<AvailabilitySlot>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                        FROM availability_slot
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<List<AvailabilitySlot>> findBySpecialistId(UUID specialistId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                        FROM availability_slot
                        WHERE specialist_id = $1
                        ORDER BY start_at ASC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(specialistId, limit, offset))
                .map(this::mapList);
    }

    public Uni<List<AvailabilitySlot>> findAvailableBySpecialistId(UUID specialistId) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                        FROM availability_slot
                        WHERE specialist_id = $1 AND status = 'AVAILABLE'
                        ORDER BY start_at ASC
                        """)
                .execute(Tuple.of(specialistId))
                .map(this::mapList);
    }

    public Uni<Long> countBySpecialistId(UUID specialistId) {
        return client.preparedQuery("SELECT count(*) AS total FROM availability_slot WHERE specialist_id = $1")
                .execute(Tuple.of(specialistId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Void> updateStatus(UUID id, SlotStatus status) {
        Tuple params = Tuple.tuple();
        params.addString(status.name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        params.addUUID(id);

        return client.preparedQuery("""
                        UPDATE availability_slot
                        SET status = $1, updated_at = $2
                        WHERE id = $3
                        """)
                .execute(params)
                .replaceWithVoid();
    }

    /**
     * Atomically reserves a slot only if its current status is AVAILABLE.
     * Uses a conditional UPDATE to prevent race conditions when two concurrent
     * requests try to reserve the same slot.
     *
     * @return true if the slot was successfully reserved, false if it was already taken
     */
    public Uni<Boolean> reserveIfAvailable(UUID id) {
        Tuple params = Tuple.tuple();
        params.addOffsetDateTime(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        params.addUUID(id);

        return client.preparedQuery("""
                        UPDATE availability_slot
                        SET status = 'RESERVED', updated_at = $1
                        WHERE id = $2 AND status = 'AVAILABLE'
                        """)
                .execute(params)
                .map(rows -> rows.rowCount() > 0);
    }

    public Uni<Boolean> delete(UUID id) {
        return client.preparedQuery("DELETE FROM availability_slot WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    /**
     * Checks whether a specialist already has a non-cancelled slot that overlaps
     * with the given time range using PostgreSQL tstzrange overlap operator.
     */
    public Uni<Boolean> hasOverlappingSlot(UUID specialistId, Instant startAt, Instant endAt) {
        Tuple params = Tuple.tuple();
        params.addUUID(specialistId);
        params.addOffsetDateTime(OffsetDateTime.ofInstant(startAt, ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(endAt, ZoneOffset.UTC));

        return client.preparedQuery("""
                        SELECT EXISTS (
                            SELECT 1 FROM availability_slot
                            WHERE specialist_id = $1
                              AND status != 'CANCELLED'
                              AND tstzrange(start_at, end_at) && tstzrange($2, $3)
                        ) AS has_overlap
                        """)
                .execute(params)
                .map(rows -> rows.iterator().next().getBoolean("has_overlap"));
    }

    /**
     * Returns all slots for a specialist within a date range, ordered by start_at ASC.
     * Useful for calendar views.
     */
    public Uni<List<AvailabilitySlot>> findBySpecialistIdAndDateRange(UUID specialistId,
                                                                      Instant rangeStart,
                                                                      Instant rangeEnd) {
        Tuple params = Tuple.tuple();
        params.addUUID(specialistId);
        params.addOffsetDateTime(OffsetDateTime.ofInstant(rangeStart, ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(rangeEnd, ZoneOffset.UTC));

        return client.preparedQuery("""
                        SELECT id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                        FROM availability_slot
                        WHERE specialist_id = $1
                          AND start_at >= $2
                          AND start_at < $3
                        ORDER BY start_at ASC
                        """)
                .execute(params)
                .map(this::mapList);
    }

    /**
     * Atomically inserts a batch of availability slots within a transaction.
     */
    public Uni<List<AvailabilitySlot>> saveBatch(List<AvailabilitySlot> slots) {
        if (slots.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        return client.withTransaction(conn -> {
            Uni<List<AvailabilitySlot>> result = Uni.createFrom().item(new ArrayList<>());

            for (AvailabilitySlot slot : slots) {
                Tuple params = Tuple.tuple();
                params.addUUID(slot.id());
                params.addUUID(slot.specialistId());
                params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.startAt(), ZoneOffset.UTC));
                params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.endAt(), ZoneOffset.UTC));
                params.addString(slot.status().name());
                params.addString(slot.label());
                params.addUUID(slot.recurrenceGroupId());
                params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.createdAt(), ZoneOffset.UTC));
                params.addOffsetDateTime(OffsetDateTime.ofInstant(slot.updatedAt(), ZoneOffset.UTC));

                result = result.flatMap(list ->
                        conn.preparedQuery("""
                                        INSERT INTO availability_slot (id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at)
                                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                                        RETURNING id, specialist_id, start_at, end_at, status, label, recurrence_group_id, created_at, updated_at
                                        """)
                                .execute(params)
                                .map(rows -> {
                                    list.add(mapRow(rows.iterator().next()));
                                    return list;
                                })
                );
            }

            return result;
        });
    }

    // ---- Row Mapping ----

    private AvailabilitySlot mapRow(Row row) {
        return AvailabilitySlot.restore(
                row.getUUID("id"),
                row.getUUID("specialist_id"),
                row.getOffsetDateTime("start_at").toInstant(),
                row.getOffsetDateTime("end_at").toInstant(),
                SlotStatus.valueOf(row.getString("status")),
                row.getString("label"),
                row.getUUID("recurrence_group_id"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<AvailabilitySlot> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<AvailabilitySlot> mapList(RowSet<Row> rows) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        for (Row row : rows) {
            slots.add(mapRow(row));
        }
        return slots;
    }
}
