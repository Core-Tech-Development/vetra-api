package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExamTypePricingRepository {

    private final PgPool client;

    @Inject
    public ExamTypePricingRepository(PgPool client) {
        this.client = client;
    }

    public Uni<ExamTypePricing> save(ExamTypePricing pricing) {
        Tuple params = Tuple.tuple();
        params.addUUID(pricing.id());
        params.addString(pricing.examType());
        params.addLong(pricing.priceCents());
        params.addBigDecimal(pricing.platformFeePercent());
        params.addBoolean(pricing.active());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO exam_type_pricing (id, exam_type, price_cents, platform_fee_percent,
                            active, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        RETURNING id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<ExamTypePricing> update(ExamTypePricing pricing) {
        Tuple params = Tuple.tuple();
        params.addUUID(pricing.id());
        params.addLong(pricing.priceCents());
        params.addBigDecimal(pricing.platformFeePercent());
        params.addBoolean(pricing.active());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(pricing.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE exam_type_pricing
                        SET price_cents = $2, platform_fee_percent = $3, active = $4, updated_at = $5
                        WHERE id = $1
                        RETURNING id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<ExamTypePricing>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        FROM exam_type_pricing
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<ExamTypePricing>> findByExamType(String examType) {
        return client.preparedQuery("""
                        SELECT id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        FROM exam_type_pricing
                        WHERE exam_type = $1 AND active = true
                        """)
                .execute(Tuple.of(examType))
                .map(this::mapOptional);
    }

    public Uni<List<ExamTypePricing>> findAllActive() {
        return client.preparedQuery("""
                        SELECT id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        FROM exam_type_pricing
                        WHERE active = true
                        ORDER BY exam_type
                        """)
                .execute()
                .map(this::mapList);
    }

    public Uni<List<ExamTypePricing>> findAll() {
        return client.preparedQuery("""
                        SELECT id, exam_type, price_cents, platform_fee_percent, active, created_at, updated_at
                        FROM exam_type_pricing
                        ORDER BY exam_type
                        """)
                .execute()
                .map(this::mapList);
    }

    // ---- Row Mapping ----

    private ExamTypePricing mapRow(Row row) {
        return ExamTypePricing.restore(
                row.getUUID("id"),
                row.getString("exam_type"),
                row.getLong("price_cents"),
                row.getBigDecimal("platform_fee_percent"),
                row.getBoolean("active"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<ExamTypePricing> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<ExamTypePricing> mapList(RowSet<Row> rows) {
        List<ExamTypePricing> result = new ArrayList<>();
        for (Row row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }
}
