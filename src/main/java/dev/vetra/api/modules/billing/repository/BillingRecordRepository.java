package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.domain.BillingRecordStatus;
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
public class BillingRecordRepository {

    private final PgPool client;

    @Inject
    public BillingRecordRepository(PgPool client) {
        this.client = client;
    }

    public Uni<BillingRecord> save(BillingRecord record) {
        Tuple params = Tuple.tuple();
        params.addUUID(record.id());
        params.addUUID(record.laudoId());
        params.addUUID(record.appointmentId());
        params.addUUID(record.clinicId());
        params.addUUID(record.specialistId());
        params.addString(record.examType());
        params.addLong(record.totalCents());
        params.addLong(record.platformFeeCents());
        params.addLong(record.specialistShareCents());
        params.addString(record.status().name());
        addNullableString(params, record.asaasPaymentId());
        addNullableString(params, record.errorMessage());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(record.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(record.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO billing_record (id, laudo_id, appointment_id, clinic_id, specialist_id,
                            exam_type, total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
                        RETURNING id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<BillingRecord> update(BillingRecord record) {
        Tuple params = Tuple.tuple();
        params.addUUID(record.id());
        params.addString(record.status().name());
        addNullableString(params, record.asaasPaymentId());
        addNullableString(params, record.errorMessage());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(record.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE billing_record
                        SET status = $2, asaas_payment_id = $3, error_message = $4, updated_at = $5
                        WHERE id = $1
                        RETURNING id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<BillingRecord>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<BillingRecord>> findByLaudoId(UUID laudoId) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE laudo_id = $1
                        """)
                .execute(Tuple.of(laudoId))
                .map(this::mapOptional);
    }

    public Uni<Optional<BillingRecord>> findByAsaasPaymentId(String asaasPaymentId) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE asaas_payment_id = $1
                        """)
                .execute(Tuple.of(asaasPaymentId))
                .map(this::mapOptional);
    }

    public Uni<List<BillingRecord>> findByClinicId(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE clinic_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countByClinicId(UUID clinicId) {
        return client.preparedQuery("SELECT count(*) AS total FROM billing_record WHERE clinic_id = $1")
                .execute(Tuple.of(clinicId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<List<BillingRecord>> findByStatus(BillingRecordStatus status) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE status = $1
                        ORDER BY created_at ASC
                        """)
                .execute(Tuple.of(status.name()))
                .map(this::mapList);
    }

    public Uni<List<BillingRecord>> findPendingOlderThan(Instant cutoff) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        WHERE status = $1 AND created_at < $2
                        ORDER BY created_at ASC
                        """)
                .execute(Tuple.of(
                        BillingRecordStatus.PENDING_PAYMENT_CREATION.name(),
                        OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC)))
                .map(this::mapList);
    }

    public Uni<List<BillingRecord>> findAll(int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, laudo_id, appointment_id, clinic_id, specialist_id, exam_type,
                            total_cents, platform_fee_cents, specialist_share_cents, status,
                            asaas_payment_id, error_message, created_at, updated_at
                        FROM billing_record
                        ORDER BY created_at DESC
                        LIMIT $1 OFFSET $2
                        """)
                .execute(Tuple.of(limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countAll() {
        return client.preparedQuery("SELECT count(*) AS total FROM billing_record")
                .execute()
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> sumTotalCentsByStatus(BillingRecordStatus status) {
        return client.preparedQuery(
                        "SELECT COALESCE(SUM(total_cents), 0) AS total FROM billing_record WHERE status = $1")
                .execute(Tuple.of(status.name()))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> sumPlatformFeeCentsByStatus(BillingRecordStatus status) {
        return client.preparedQuery(
                        "SELECT COALESCE(SUM(platform_fee_cents), 0) AS total FROM billing_record WHERE status = $1")
                .execute(Tuple.of(status.name()))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> countByStatus(BillingRecordStatus status) {
        return client.preparedQuery("SELECT count(*) AS total FROM billing_record WHERE status = $1")
                .execute(Tuple.of(status.name()))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    // ---- Row Mapping ----

    private BillingRecord mapRow(Row row) {
        return BillingRecord.restore(
                row.getUUID("id"),
                row.getUUID("laudo_id"),
                row.getUUID("appointment_id"),
                row.getUUID("clinic_id"),
                row.getUUID("specialist_id"),
                row.getString("exam_type"),
                row.getLong("total_cents"),
                row.getLong("platform_fee_cents"),
                row.getLong("specialist_share_cents"),
                BillingRecordStatus.valueOf(row.getString("status")),
                row.getString("asaas_payment_id"),
                row.getString("error_message"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<BillingRecord> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<BillingRecord> mapList(RowSet<Row> rows) {
        List<BillingRecord> result = new ArrayList<>();
        for (Row row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    private void addNullableString(Tuple params, String value) {
        if (value != null) {
            params.addString(value);
        } else {
            params.addValue(null);
        }
    }
}
