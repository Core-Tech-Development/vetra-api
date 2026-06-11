package dev.vetra.api.modules.clinic.repository;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
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
public class ClinicRepository {

    private final PgPool client;

    @Inject
    public ClinicRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Clinic> save(Clinic clinic) {
        Tuple params = Tuple.tuple();
        params.addUUID(clinic.id());
        params.addString(clinic.name());
        params.addString(clinic.document());
        params.addString(clinic.email());
        params.addString(clinic.phone());
        params.addString(clinic.address());
        params.addString(clinic.city());
        params.addString(clinic.state());
        params.addString(clinic.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(clinic.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(clinic.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO clinic (id, name, document, email, phone, address, city, state, status, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
                        RETURNING id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Clinic>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        FROM clinic
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<Clinic>> findByDocument(String document) {
        return client.preparedQuery("""
                        SELECT id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        FROM clinic
                        WHERE document = $1
                        """)
                .execute(Tuple.of(document))
                .map(this::mapOptional);
    }

    public Uni<Optional<Clinic>> findByEmail(String email) {
        return client.preparedQuery("""
                        SELECT id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        FROM clinic
                        WHERE email = $1
                        """)
                .execute(Tuple.of(email))
                .map(this::mapOptional);
    }

    public Uni<List<Clinic>> findAll(int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        FROM clinic
                        ORDER BY created_at DESC
                        LIMIT $1 OFFSET $2
                        """)
                .execute(Tuple.of(limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> count() {
        return client.preparedQuery("SELECT count(*) AS total FROM clinic")
                .execute(Tuple.tuple())
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Clinic> update(Clinic clinic) {
        Tuple params = Tuple.tuple();
        params.addUUID(clinic.id());
        params.addString(clinic.name());
        params.addString(clinic.document());
        params.addString(clinic.email());
        params.addString(clinic.phone());
        params.addString(clinic.address());
        params.addString(clinic.city());
        params.addString(clinic.state());
        params.addString(clinic.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(clinic.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE clinic
                        SET name = $2, document = $3, email = $4, phone = $5, address = $6,
                            city = $7, state = $8, status = $9, updated_at = $10
                        WHERE id = $1
                        RETURNING id, name, document, email, phone, address, city, state, status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    // ---- Row Mapping ----

    private Clinic mapRow(Row row) {
        return Clinic.restore(
                row.getUUID("id"),
                row.getString("name"),
                row.getString("document"),
                row.getString("email"),
                row.getString("phone"),
                row.getString("address"),
                row.getString("city"),
                row.getString("state"),
                ClinicStatus.valueOf(row.getString("status")),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Clinic> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Clinic> mapList(RowSet<Row> rows) {
        List<Clinic> clinics = new ArrayList<>();
        for (Row row : rows) {
            clinics.add(mapRow(row));
        }
        return clinics;
    }
}
