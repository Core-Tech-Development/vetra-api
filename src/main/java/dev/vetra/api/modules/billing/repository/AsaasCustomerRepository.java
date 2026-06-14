package dev.vetra.api.modules.billing.repository;

import dev.vetra.api.modules.billing.domain.AsaasCustomer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AsaasCustomerRepository {

    private final PgPool client;

    @Inject
    public AsaasCustomerRepository(PgPool client) {
        this.client = client;
    }

    public Uni<AsaasCustomer> save(AsaasCustomer customer) {
        Tuple params = Tuple.tuple();
        params.addUUID(customer.id());
        params.addUUID(customer.clinicId());
        params.addString(customer.asaasCustomerId());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(customer.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO asaas_customer (id, clinic_id, asaas_customer_id, created_at)
                        VALUES ($1, $2, $3, $4)
                        RETURNING id, clinic_id, asaas_customer_id, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<AsaasCustomer>> findByClinicId(UUID clinicId) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, asaas_customer_id, created_at
                        FROM asaas_customer
                        WHERE clinic_id = $1
                        """)
                .execute(Tuple.of(clinicId))
                .map(this::mapOptional);
    }

    // ---- Row Mapping ----

    private AsaasCustomer mapRow(Row row) {
        return AsaasCustomer.restore(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getString("asaas_customer_id"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private Optional<AsaasCustomer> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }
}
