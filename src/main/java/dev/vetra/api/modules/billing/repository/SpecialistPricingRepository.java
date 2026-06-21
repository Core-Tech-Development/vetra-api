package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.SpecialistPricing;
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
public class SpecialistPricingRepository {

    private final PgPool client;

    @Inject
    public SpecialistPricingRepository(PgPool client) {
        this.client = client;
    }

    public Uni<SpecialistPricing> save(SpecialistPricing pricing) {
        Tuple params = Tuple.tuple();
        params.addUUID(pricing.id());
        params.addUUID(pricing.specialistId());
        params.addString(pricing.examType());
        params.addLong(pricing.priceCents());
        params.addBoolean(pricing.active());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO specialist_pricing (id, specialist_id, exam_type, price_cents,
                            active, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        RETURNING id, specialist_id, exam_type, price_cents, active, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<SpecialistPricing>> findBySpecialistIdAndExamType(UUID specialistId, String examType) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, exam_type, price_cents, active, created_at, updated_at
                        FROM specialist_pricing
                        WHERE specialist_id = $1 AND exam_type = $2 AND active = true
                        """)
                .execute(Tuple.of(specialistId, examType))
                .map(this::mapOptional);
    }

    public Uni<SpecialistPricing> upsert(SpecialistPricing pricing) {
        Tuple params = Tuple.tuple();
        params.addUUID(pricing.id());
        params.addUUID(pricing.specialistId());
        params.addString(pricing.examType());
        params.addLong(pricing.priceCents());
        params.addBoolean(pricing.active());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO specialist_pricing (id, specialist_id, exam_type, price_cents,
                            active, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        ON CONFLICT (specialist_id, exam_type)
                        DO UPDATE SET price_cents = EXCLUDED.price_cents,
                                      active = EXCLUDED.active,
                                      updated_at = EXCLUDED.updated_at
                        RETURNING id, specialist_id, exam_type, price_cents, active, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<SpecialistPricing>> findBySpecialistId(UUID specialistId) {
        return client.preparedQuery("""
                        SELECT id, specialist_id, exam_type, price_cents, active, created_at, updated_at
                        FROM specialist_pricing
                        WHERE specialist_id = $1 AND active = true
                        ORDER BY exam_type
                        """)
                .execute(Tuple.of(specialistId))
                .map(this::mapList);
    }

    // ---- Row Mapping ----

    private SpecialistPricing mapRow(Row row) {
        return SpecialistPricing.restore(
                row.getUUID("id"),
                row.getUUID("specialist_id"),
                row.getString("exam_type"),
                row.getLong("price_cents"),
                row.getBoolean("active"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<SpecialistPricing> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<SpecialistPricing> mapList(RowSet<Row> rows) {
        List<SpecialistPricing> result = new ArrayList<>();
        for (Row row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }
}
