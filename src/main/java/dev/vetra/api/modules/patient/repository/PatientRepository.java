package dev.vetra.api.modules.patient.repository;

import dev.vetra.api.modules.patient.domain.Patient;
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
public class PatientRepository {

    private final PgPool client;

    @Inject
    public PatientRepository(PgPool client) {
        this.client = client;
    }

    public Uni<Patient> save(Patient patient) {
        Tuple params = Tuple.tuple();
        params.addUUID(patient.id());
        params.addUUID(patient.clinicId());
        params.addUUID(patient.tutorId());
        params.addString(patient.name());
        params.addString(patient.species());
        params.addString(patient.breed());
        params.addString(patient.sex());
        params.addLocalDate(patient.birthDate());
        params.addBigDecimal(patient.weightKg());
        params.addBoolean(patient.neutered());
        params.addString(patient.microchip());
        params.addString(patient.clinicalNotes());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(patient.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(patient.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO patient (id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                                             weight_kg, neutered, microchip, clinical_notes, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
                        RETURNING id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                                  weight_kg, neutered, microchip, clinical_notes, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<Patient>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                               weight_kg, neutered, microchip, clinical_notes, created_at, updated_at
                        FROM patient
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<List<Patient>> findByClinicId(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                               weight_kg, neutered, microchip, clinical_notes, created_at, updated_at
                        FROM patient
                        WHERE clinic_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(this::mapList);
    }

    public Uni<List<Patient>> findByTutorId(UUID tutorId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                               weight_kg, neutered, microchip, clinical_notes, created_at, updated_at
                        FROM patient
                        WHERE tutor_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(tutorId, limit, offset))
                .map(this::mapList);
    }

    public Uni<Long> countByClinicId(UUID clinicId) {
        return client.preparedQuery("SELECT count(*) AS total FROM patient WHERE clinic_id = $1")
                .execute(Tuple.of(clinicId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Long> countByTutorId(UUID tutorId) {
        return client.preparedQuery("SELECT count(*) AS total FROM patient WHERE tutor_id = $1")
                .execute(Tuple.of(tutorId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<Patient> update(Patient patient) {
        Tuple params = Tuple.tuple();
        params.addUUID(patient.id());
        params.addString(patient.name());
        params.addString(patient.species());
        params.addString(patient.breed());
        params.addString(patient.sex());
        params.addLocalDate(patient.birthDate());
        params.addBigDecimal(patient.weightKg());
        params.addBoolean(patient.neutered());
        params.addString(patient.microchip());
        params.addString(patient.clinicalNotes());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(patient.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE patient
                        SET name = $2, species = $3, breed = $4, sex = $5, birth_date = $6,
                            weight_kg = $7, neutered = $8, microchip = $9, clinical_notes = $10, updated_at = $11
                        WHERE id = $1
                        RETURNING id, clinic_id, tutor_id, name, species, breed, sex, birth_date,
                                  weight_kg, neutered, microchip, clinical_notes, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Boolean> deleteById(UUID id) {
        return client.preparedQuery("DELETE FROM patient WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    // ---- Row Mapping ----

    private Patient mapRow(Row row) {
        return Patient.restore(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getUUID("tutor_id"),
                row.getString("name"),
                row.getString("species"),
                row.getString("breed"),
                row.getString("sex"),
                row.getLocalDate("birth_date"),
                row.getBigDecimal("weight_kg"),
                row.getBoolean("neutered"),
                row.getString("microchip"),
                row.getString("clinical_notes"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private Optional<Patient> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<Patient> mapList(RowSet<Row> rows) {
        List<Patient> patients = new ArrayList<>();
        for (Row row : rows) {
            patients.add(mapRow(row));
        }
        return patients;
    }
}
