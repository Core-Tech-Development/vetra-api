package dev.vetra.api.modules.imaging.repository;

import dev.vetra.api.modules.imaging.domain.ExamFile;
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
public class ExamFileRepository {

    private final PgPool client;

    @Inject
    public ExamFileRepository(PgPool client) {
        this.client = client;
    }

    public Uni<ExamFile> save(ExamFile file) {
        Tuple params = Tuple.tuple();
        params.addUUID(file.id());
        params.addUUID(file.appointmentId());
        params.addString(file.fileName());
        params.addString(file.fileType());
        params.addString(file.contentType());
        params.addString(file.storageKey());
        params.addLong(file.sizeBytes());
        params.addString(file.uploadedBy());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(file.createdAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO exam_file (id, appointment_id, file_name, file_type, content_type, storage_key, size_bytes, uploaded_by, created_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                        RETURNING id, appointment_id, file_name, file_type, content_type, storage_key, size_bytes, uploaded_by, created_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<ExamFile>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, file_name, file_type, content_type, storage_key, size_bytes, uploaded_by, created_at
                        FROM exam_file
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<List<ExamFile>> findByAppointmentId(UUID appointmentId) {
        return client.preparedQuery("""
                        SELECT id, appointment_id, file_name, file_type, content_type, storage_key, size_bytes, uploaded_by, created_at
                        FROM exam_file
                        WHERE appointment_id = $1
                        ORDER BY created_at DESC
                        """)
                .execute(Tuple.of(appointmentId))
                .map(this::mapList);
    }

    public Uni<Boolean> deleteById(UUID id) {
        return client.preparedQuery("DELETE FROM exam_file WHERE id = $1")
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    // ---- Row Mapping ----

    private ExamFile mapRow(Row row) {
        return ExamFile.restore(
                row.getUUID("id"),
                row.getUUID("appointment_id"),
                row.getString("file_name"),
                row.getString("file_type"),
                row.getString("content_type"),
                row.getString("storage_key"),
                row.getLong("size_bytes"),
                row.getString("uploaded_by"),
                row.getOffsetDateTime("created_at").toInstant()
        );
    }

    private Optional<ExamFile> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<ExamFile> mapList(RowSet<Row> rows) {
        List<ExamFile> files = new ArrayList<>();
        for (Row row : rows) {
            files.add(mapRow(row));
        }
        return files;
    }
}
