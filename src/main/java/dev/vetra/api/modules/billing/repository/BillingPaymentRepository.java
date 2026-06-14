package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.BillingPayment;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BillingPaymentRepository {

    private final PgPool client;

    @Inject
    public BillingPaymentRepository(PgPool client) {
        this.client = client;
    }

    public Uni<BillingPayment> save(BillingPayment payment) {
        Tuple params = Tuple.tuple();
        params.addUUID(payment.id());
        params.addUUID(payment.billingRecordId());
        params.addString(payment.asaasPaymentId());
        params.addString(payment.status());
        addNullableString(params, payment.billingType());
        addNullableString(params, payment.pixQrCode());
        addNullableString(params, payment.pixCopyPaste());
        addNullableString(params, payment.boletoUrl());
        addNullableString(params, payment.invoiceUrl());
        addNullableLocalDate(params, payment.dueDate());
        addNullableOffsetDateTime(params, payment.paidAt());
        params.addLong(payment.valueCents());
        addNullableLong(params, payment.netValueCents());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(payment.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(payment.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO billing_payment (id, billing_record_id, asaas_payment_id, status,
                            billing_type, pix_qr_code, pix_copy_paste, boleto_url, invoice_url,
                            due_date, paid_at, value_cents, net_value_cents, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
                        RETURNING id, billing_record_id, asaas_payment_id, status, billing_type,
                            pix_qr_code, pix_copy_paste, boleto_url, invoice_url, due_date, paid_at,
                            value_cents, net_value_cents, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<BillingPayment> update(BillingPayment payment) {
        Tuple params = Tuple.tuple();
        params.addUUID(payment.id());
        params.addString(payment.status());
        addNullableString(params, payment.pixQrCode());
        addNullableString(params, payment.pixCopyPaste());
        addNullableString(params, payment.boletoUrl());
        addNullableString(params, payment.invoiceUrl());
        addNullableOffsetDateTime(params, payment.paidAt());
        addNullableLong(params, payment.netValueCents());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(payment.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE billing_payment
                        SET status = $2, pix_qr_code = $3, pix_copy_paste = $4, boleto_url = $5,
                            invoice_url = $6, paid_at = $7, net_value_cents = $8, updated_at = $9
                        WHERE id = $1
                        RETURNING id, billing_record_id, asaas_payment_id, status, billing_type,
                            pix_qr_code, pix_copy_paste, boleto_url, invoice_url, due_date, paid_at,
                            value_cents, net_value_cents, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<BillingPayment>> findByBillingRecordId(UUID billingRecordId) {
        return client.preparedQuery("""
                        SELECT id, billing_record_id, asaas_payment_id, status, billing_type,
                            pix_qr_code, pix_copy_paste, boleto_url, invoice_url, due_date, paid_at,
                            value_cents, net_value_cents, created_at, updated_at
                        FROM billing_payment
                        WHERE billing_record_id = $1
                        """)
                .execute(Tuple.of(billingRecordId))
                .map(this::mapOptional);
    }

    public Uni<Optional<BillingPayment>> findByAsaasPaymentId(String asaasPaymentId) {
        return client.preparedQuery("""
                        SELECT id, billing_record_id, asaas_payment_id, status, billing_type,
                            pix_qr_code, pix_copy_paste, boleto_url, invoice_url, due_date, paid_at,
                            value_cents, net_value_cents, created_at, updated_at
                        FROM billing_payment
                        WHERE asaas_payment_id = $1
                        """)
                .execute(Tuple.of(asaasPaymentId))
                .map(this::mapOptional);
    }

    // ---- Row Mapping ----

    private BillingPayment mapRow(Row row) {
        OffsetDateTime paidAtOdt = row.getOffsetDateTime("paid_at");
        Instant paidAt = paidAtOdt != null ? paidAtOdt.toInstant() : null;

        LocalDate dueDate = row.getLocalDate("due_date");

        Long netValueCents = row.getLong("net_value_cents");

        return BillingPayment.restore(
                row.getUUID("id"),
                row.getUUID("billing_record_id"),
                row.getString("asaas_payment_id"),
                row.getString("status"),
                row.getString("billing_type"),
                row.getString("pix_qr_code"),
                row.getString("pix_copy_paste"),
                row.getString("boleto_url"),
                row.getString("invoice_url"),
                dueDate,
                paidAt,
                row.getLong("value_cents"),
                netValueCents,
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<BillingPayment> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<BillingPayment> mapList(RowSet<Row> rows) {
        List<BillingPayment> result = new ArrayList<>();
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

    private void addNullableOffsetDateTime(Tuple params, Instant instant) {
        if (instant != null) {
            params.addOffsetDateTime(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
        } else {
            params.addOffsetDateTime(null);
        }
    }

    private void addNullableLocalDate(Tuple params, LocalDate date) {
        if (date != null) {
            params.addLocalDate(date);
        } else {
            params.addValue(null);
        }
    }

    private void addNullableLong(Tuple params, Long value) {
        if (value != null) {
            params.addLong(value);
        } else {
            params.addValue(null);
        }
    }
}
