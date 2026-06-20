package dev.vetra.api.shared;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Helper for inserting prerequisite data in integration tests.
 * Uses PgPool directly to insert records needed to satisfy FK constraints.
 */
@ApplicationScoped
public class TestDataFactory {

    private final PgPool client;

    @Inject
    public TestDataFactory(PgPool client) {
        this.client = client;
    }

    public Uni<UUID> insertClinic(UUID id, String name, String document, String status) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        params.addString(name);
        params.addString(document);
        params.addString(name.toLowerCase() + "@test.com");
        params.addString("11999999999");
        params.addString("SP");
        params.addString("SP");
        params.addString(status);
        params.addOffsetDateTime(now);
        params.addOffsetDateTime(now);

        return client.preparedQuery("""
                        INSERT INTO clinic (id, name, document, email, phone, city, state, status, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
                        """)
                .execute(params)
                .map(r -> id);
    }

    public Uni<UUID> insertActiveClinic() {
        return insertClinic(UUID.randomUUID(), "Test Clinic", UUID.randomUUID().toString().substring(0, 14), "ACTIVE");
    }

    public Uni<UUID> insertTutor(UUID id, UUID clinicId, String name) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        params.addUUID(clinicId);
        params.addString(name);
        params.addString("11999999999");
        params.addString(name.toLowerCase().replace(" ", "") + "@test.com");
        params.addOffsetDateTime(now);
        params.addOffsetDateTime(now);

        return client.preparedQuery("""
                        INSERT INTO tutor (id, clinic_id, name, phone, email, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        """)
                .execute(params)
                .map(r -> id);
    }

    public Uni<UUID> insertSpecialist(UUID id, String name) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        params.addString("user-" + id);
        params.addString(name);
        params.addString(name.toLowerCase().replace(" ", "") + "@test.com");
        params.addString("11999999999");
        params.addString("CRMV-" + id.toString().substring(0, 5));
        params.addString("SP");
        params.addString("ABDOMINAL_ULTRASOUND");
        params.addString("SP");
        params.addString("SP");
        params.addString("ACTIVE");
        params.addOffsetDateTime(now);
        params.addOffsetDateTime(now);

        return client.preparedQuery("""
                        INSERT INTO specialist (id, user_id, name, email, phone, crmv, crmv_state, specialty, base_city, base_state, status, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
                        """)
                .execute(params)
                .map(r -> id);
    }

    public Uni<UUID> insertExamRequest(UUID id, UUID clinicId, UUID patientId, String status) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        params.addUUID(clinicId);
        params.addUUID(patientId);
        params.addString("ABDOMINAL_ULTRASOUND");
        params.addString("ROUTINE");
        params.addString(status);
        params.addString("test-user");
        params.addOffsetDateTime(now);
        params.addOffsetDateTime(now);

        return client.preparedQuery("""
                        INSERT INTO exam_request (id, clinic_id, patient_id, exam_type, priority, status, requested_by, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                        """)
                .execute(params)
                .map(r -> id);
    }

    public Uni<Void> truncateAll() {
        return client.query("""
                        TRUNCATE appointment, exam_request, patient, tutor, specialist, clinic CASCADE
                        """)
                .execute()
                .replaceWithVoid();
    }
}
