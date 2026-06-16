package dev.vetra.api.modules.clinic.repository;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.domain.ClinicStaffRole;
import dev.vetra.api.modules.clinic.domain.ClinicStaffStatus;
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
public class ClinicStaffRepository {

    private final PgPool client;

    @Inject
    public ClinicStaffRepository(PgPool client) {
        this.client = client;
    }

    public Uni<ClinicStaff> save(ClinicStaff staff) {
        Tuple params = Tuple.tuple();
        params.addUUID(staff.id());
        params.addUUID(staff.clinicId());
        params.addString(staff.userId());
        params.addString(staff.name());
        params.addString(staff.email());
        params.addString(staff.phone());
        params.addString(staff.role().name());
        params.addString(staff.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(staff.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(staff.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO clinic_staff (id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
                        RETURNING id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<ClinicStaff>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        FROM clinic_staff
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<ClinicStaff>> findByUserId(String userId) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        FROM clinic_staff
                        WHERE user_id = $1
                        """)
                .execute(Tuple.of(userId))
                .map(this::mapOptional);
    }

    public Uni<Optional<ClinicStaff>> findByEmail(String email) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        FROM clinic_staff
                        WHERE email = $1
                        """)
                .execute(Tuple.of(email))
                .map(this::mapOptional);
    }

    public Uni<List<ClinicStaff>> findByClinicId(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        FROM clinic_staff
                        WHERE clinic_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countByClinicId(UUID clinicId) {
        return client.preparedQuery("SELECT count(*) AS total FROM clinic_staff WHERE clinic_id = $1")
                .execute(Tuple.of(clinicId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<ClinicStaff> update(ClinicStaff staff) {
        Tuple params = Tuple.tuple();
        params.addUUID(staff.id());
        params.addString(staff.name());
        params.addString(staff.phone());
        params.addString(staff.role().name());
        params.addString(staff.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(staff.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE clinic_staff
                        SET name = $2, phone = $3, role = $4, status = $5, updated_at = $6
                        WHERE id = $1
                        RETURNING id, clinic_id, user_id, name, email, phone, role, status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Void> updateUserId(UUID id, String userId) {
        return client.preparedQuery("UPDATE clinic_staff SET user_id = $2 WHERE id = $1")
                .execute(Tuple.of(id, userId))
                .replaceWithVoid();
    }

    public Uni<List<String>> findAdminUserIdsByClinicId(UUID clinicId) {
        return client.preparedQuery("""
                        SELECT user_id FROM clinic_staff
                        WHERE clinic_id = $1 AND status = 'ACTIVE'
                        """)
                .execute(Tuple.of(clinicId))
                .map(rows -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        String uid = row.getString("user_id");
                        if (uid != null) ids.add(uid);
                    }
                    return ids;
                });
    }

    // ---- Row Mapping ----

    private ClinicStaff mapRow(Row row) {
        return ClinicStaff.restore(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getString("user_id"),
                row.getString("name"),
                row.getString("email"),
                row.getString("phone"),
                ClinicStaffRole.valueOf(row.getString("role")),
                ClinicStaffStatus.valueOf(row.getString("status")),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<ClinicStaff> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<ClinicStaff> mapList(RowSet<Row> rows) {
        List<ClinicStaff> list = new ArrayList<>();
        for (Row row : rows) {
            list.add(mapRow(row));
        }
        return list;
    }
}
