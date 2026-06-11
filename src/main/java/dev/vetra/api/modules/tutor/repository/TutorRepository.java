package dev.vetra.api.modules.tutor.repository;

import dev.vetra.api.modules.tutor.domain.Tutor;
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
public class TutorRepository {

    private final PgPool client;

    @Inject
    public TutorRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Tutor> save(Tutor tutor) {
        Tuple params = Tuple.tuple();
        params.addUUID(tutor.id());
        params.addUUID(tutor.clinicId());
        params.addString(tutor.name());
        params.addString(tutor.phone());
        params.addString(tutor.email());
        params.addString(tutor.document());
        params.addString(tutor.address());
        params.addString(tutor.city());
        params.addString(tutor.state());
        params.addString(tutor.zipCode());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(tutor.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(tutor.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO tutor (id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
                        RETURNING id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Tutor>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at
                        FROM tutor
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<Tutor>> findByDocumentAndClinicId(String document, UUID clinicId) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at
                        FROM tutor
                        WHERE document = $1 AND clinic_id = $2
                        """)
                .execute(Tuple.of(document, clinicId))
                .map(this::mapOptional);
    }

    public Uni<List<Tutor>> findByClinicId(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at
                        FROM tutor
                        WHERE clinic_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countByClinicId(UUID clinicId) {
        return client.preparedQuery("SELECT count(*) AS total FROM tutor WHERE clinic_id = $1")
                .execute(Tuple.of(clinicId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Tutor> update(Tutor tutor) {
        Tuple params = Tuple.tuple();
        params.addUUID(tutor.id());
        params.addString(tutor.name());
        params.addString(tutor.phone());
        params.addString(tutor.email());
        params.addString(tutor.document());
        params.addString(tutor.address());
        params.addString(tutor.city());
        params.addString(tutor.state());
        params.addString(tutor.zipCode());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(tutor.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE tutor
                        SET name = $2, phone = $3, email = $4, document = $5, address = $6, city = $7, state = $8, zip_code = $9, updated_at = $10
                        WHERE id = $1
                        RETURNING id, clinic_id, name, phone, email, document, address, city, state, zip_code, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Boolean> deleteById(UUID id) {
        return client.preparedQuery("DELETE FROM tutor WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    // ---- Row Mapping ----

    private Tutor mapRow(Row row) {
        return Tutor.restore(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getString("name"),
                row.getString("phone"),
                row.getString("email"),
                row.getString("document"),
                row.getString("address"),
                row.getString("city"),
                row.getString("state"),
                row.getString("zip_code"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Tutor> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Tutor> mapList(RowSet<Row> rows) {
        List<Tutor> tutors = new ArrayList<>();
        for (Row row : rows) {
            tutors.add(mapRow(row));
        }
        return tutors;
    }
}
