package dev.vetra.api.modules.specialist.repository;

import dev.vetra.api.modules.specialist.domain.CoverageArea;
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
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CoverageAreaRepository {

    private final PgPool client;

    @Inject
    public CoverageAreaRepository(PgPool client) {
        this.client = client;
    }

    public Uni<CoverageArea> save(CoverageArea area) {
        Tuple params = Tuple.tuple();
        params.addUUID(area.id());
        params.addUUID(area.specialistId());
        params.addString(area.city());
        params.addString(area.state());
        params.addInteger(area.radiusKm());
        params.addBoolean(area.active());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(area.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO specialist_coverage_area (id, specialist_id, city, state, radius_km, active, created_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        RETURNING id, specialist_id, city, state, radius_km, active, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<CoverageArea>> findBySpecialistId(UUID specialistId) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, city, state, radius_km, active, created_at
                        FROM specialist_coverage_area
                        WHERE specialist_id = $1
                        ORDER BY created_at DESC
                        """)
                .execute(Tuple.of(specialistId))
                .map(this::mapList);
    }

    public Uni<Optional<CoverageArea>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, city, state, radius_km, active, created_at
                        FROM specialist_coverage_area
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<CoverageArea> updateActive(UUID id, boolean active) {
        return client.preparedQuery("""
                        UPDATE specialist_coverage_area
                        SET active = $2
                        WHERE id = $1
                        RETURNING id, specialist_id, city, state, radius_km, active, created_at
                        """)
                .execute(Tuple.of(id, active))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        throw new RuntimeException("Coverage area not found");
                    }
                    return mapRow(rows.iterator().next());
                });
    }

    public Uni<Boolean> delete(UUID id) {
        return client.preparedQuery("""
                        DELETE FROM specialist_coverage_area
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    // ---- Row Mapping ----

    private CoverageArea mapRow(Row row) {
        return CoverageArea.restore(
                row.getUUID("id"),
                row.getUUID("specialist_id"),
                row.getString("city"),
                row.getString("state"),
                row.getInteger("radius_km"),
                row.getBoolean("active"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private Optional<CoverageArea> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<CoverageArea> mapList(RowSet<Row> rows) {
        List<CoverageArea> areas = new ArrayList<>();
        for (Row row : rows) {
            areas.add(mapRow(row));
        }
        return areas;
    }
}
