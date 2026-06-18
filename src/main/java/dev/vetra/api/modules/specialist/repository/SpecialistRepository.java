package dev.vetra.api.modules.specialist.repository;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.domain.SpecialistStatus;
import dev.vetra.api.modules.specialist.domain.Specialty;
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
public class SpecialistRepository {

    private final PgPool client;

    @Inject
    public SpecialistRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Specialist> save(Specialist specialist) {
        Tuple params = Tuple.tuple();
        params.addUUID(specialist.id());
        params.addString(specialist.userId());
        params.addString(specialist.name());
        params.addString(specialist.email());
        params.addString(specialist.phone());
        params.addString(specialist.crmv());
        params.addString(specialist.crmvState());
        params.addString(specialist.specialty().name());
        params.addString(specialist.baseCity());
        params.addString(specialist.baseState());
        params.addInteger(specialist.maxTravelRadiusKm());
        params.addBoolean(specialist.hasOwnEquipment());
        params.addString(specialist.bio());
        params.addString(specialist.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(specialist.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(specialist.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO specialist (id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
                        RETURNING id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Specialist>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        FROM specialist
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<Optional<Specialist>> findByUserId(String userId) {
        return client.preparedQuery("""
                        SELECT id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        FROM specialist
                        WHERE user_id = $1
                        """)
                .execute(Tuple.of(userId))
                .map(this::mapOptional);
    }

    public Uni<Optional<Specialist>> findByCrmv(String crmv, String crmvState) {
        return client.preparedQuery("""
                        SELECT id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        FROM specialist
                        WHERE crmv = $1 AND crmv_state = $2
                        """)
                .execute(Tuple.of(crmv, crmvState))
                .map(this::mapOptional);
    }

    public Uni<List<Specialist>> findAll(int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        FROM specialist
                        ORDER BY created_at DESC
                        LIMIT $1 OFFSET $2
                        """)
                .execute(Tuple.of(limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> count() {
        return client.preparedQuery("SELECT count(*) AS total FROM specialist")
                .execute(Tuple.tuple())
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Specialist> update(Specialist specialist) {
        Tuple params = Tuple.tuple();
        params.addUUID(specialist.id());
        params.addString(specialist.name());
        params.addString(specialist.email());
        params.addString(specialist.phone());
        params.addString(specialist.crmv());
        params.addString(specialist.crmvState());
        params.addString(specialist.specialty().name());
        params.addString(specialist.baseCity());
        params.addString(specialist.baseState());
        params.addInteger(specialist.maxTravelRadiusKm());
        params.addBoolean(specialist.hasOwnEquipment());
        params.addString(specialist.bio());
        params.addString(specialist.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(specialist.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE specialist
                        SET name = $2, email = $3, phone = $4, crmv = $5, crmv_state = $6, specialty = $7,
                            base_city = $8, base_state = $9, max_travel_radius_km = $10, has_own_equipment = $11,
                            bio = $12, status = $13, updated_at = $14
                        WHERE id = $1
                        RETURNING id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Void> updateUserId(UUID id, String userId) {
        return client.preparedQuery("""
                        UPDATE specialist
                        SET user_id = $2, updated_at = now()
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id, userId))
                .replaceWithVoid();
    }

    public Uni<List<Specialist>> findByStatus(SpecialistStatus status, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, user_id, name, email, phone, crmv, crmv_state, specialty,
                            base_city, base_state, max_travel_radius_km, has_own_equipment, bio,
                            status, created_at, updated_at
                        FROM specialist
                        WHERE status = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(status.name(), limit, offset))
                .map(this::mapList);
    }

    public Uni<List<UUID>> findActiveSpecialistIds() {
        return client.preparedQuery("""
                        SELECT id FROM specialist WHERE status = 'ACTIVE'
                        """)
                .execute(Tuple.tuple())
                .map(rows -> {
                    List<UUID> ids = new ArrayList<>();
                    for (Row row : rows) {
                        ids.add(row.getUUID("id"));
                    }
                    return ids;
                });
    }

    public Uni<List<Specialist>> searchAvailable(String city, String state, String specialty) {
        if (specialty != null && !specialty.isBlank()) {
            return client.preparedQuery("""
                            SELECT DISTINCT s.id, s.user_id, s.name, s.email, s.phone, s.crmv, s.crmv_state,
                                s.specialty, s.base_city, s.base_state, s.max_travel_radius_km,
                                s.has_own_equipment, s.bio, s.status, s.created_at, s.updated_at
                            FROM specialist s
                            LEFT JOIN specialist_coverage_area sca ON sca.specialist_id = s.id AND sca.active = true
                            WHERE s.status = 'ACTIVE'
                              AND s.specialty = $1
                              AND (
                                  (s.base_city = $2 AND s.base_state = $3)
                                  OR (sca.city = $2 AND sca.state = $3)
                              )
                            ORDER BY s.created_at DESC
                            """)
                    .execute(Tuple.of(specialty, city, state))
                    .map(this::mapList);
        }
        return client.preparedQuery("""
                        SELECT DISTINCT s.id, s.user_id, s.name, s.email, s.phone, s.crmv, s.crmv_state,
                            s.specialty, s.base_city, s.base_state, s.max_travel_radius_km,
                            s.has_own_equipment, s.bio, s.status, s.created_at, s.updated_at
                        FROM specialist s
                        LEFT JOIN specialist_coverage_area sca ON sca.specialist_id = s.id AND sca.active = true
                        WHERE s.status = 'ACTIVE'
                          AND (
                              (s.base_city = $1 AND s.base_state = $2)
                              OR (sca.city = $1 AND sca.state = $2)
                          )
                        ORDER BY s.created_at DESC
                        """)
                .execute(Tuple.of(city, state))
                .map(this::mapList);
    }

    public Uni<Boolean> deleteById(UUID id) {
        return client.preparedQuery("DELETE FROM specialist WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    // ---- Row Mapping ----

    private Specialist mapRow(Row row) {
        return Specialist.restore(
                row.getUUID("id"),
                row.getString("user_id"),
                row.getString("name"),
                row.getString("email"),
                row.getString("phone"),
                row.getString("crmv"),
                row.getString("crmv_state"),
                Specialty.valueOf(row.getString("specialty")),
                row.getString("base_city"),
                row.getString("base_state"),
                row.getInteger("max_travel_radius_km"),
                row.getBoolean("has_own_equipment"),
                row.getString("bio"),
                SpecialistStatus.valueOf(row.getString("status")),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Specialist> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Specialist> mapList(RowSet<Row> rows) {
        List<Specialist> specialists = new ArrayList<>();
        for (Row row : rows) {
            specialists.add(mapRow(row));
        }
        return specialists;
    }
}
