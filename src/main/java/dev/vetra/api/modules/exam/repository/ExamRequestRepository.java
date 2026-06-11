package dev.vetra.api.modules.exam.repository;

import dev.vetra.api.modules.exam.domain.ExamPriority;
import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.domain.ExamRequestStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.vetra.api.modules.exam.dto.ExamRequestWithAppointmentResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExamRequestRepository {

    private final PgPool client;

    @Inject
    public ExamRequestRepository(PgPool client) {
        this.client = client;
    }

    public Uni<ExamRequest> save(ExamRequest examRequest) {
        Tuple params = Tuple.tuple();
        params.addUUID(examRequest.id());
        params.addUUID(examRequest.clinicId());
        params.addUUID(examRequest.patientId());
        params.addString(examRequest.examType());
        params.addString(examRequest.priority().name());
        params.addString(examRequest.diagnosticHypothesis());
        params.addString(examRequest.clinicalHistory());
        params.addString(examRequest.additionalNotes());
        params.addString(examRequest.status().name());
        params.addString(examRequest.requestedBy());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(examRequest.createdAt(), ZoneOffset.UTC));
        params.addOffsetDateTime(OffsetDateTime.ofInstant(examRequest.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        INSERT INTO exam_request (id, clinic_id, patient_id, exam_type, priority,
                                                  diagnostic_hypothesis, clinical_history, additional_notes,
                                                  status, requested_by, created_at, updated_at)
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
                        RETURNING id, clinic_id, patient_id, exam_type, priority,
                                  diagnostic_hypothesis, clinical_history, additional_notes,
                                  status, requested_by, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<Optional<ExamRequest>> findById(UUID id) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, patient_id, exam_type, priority,
                               diagnostic_hypothesis, clinical_history, additional_notes,
                               status, requested_by, created_at, updated_at
                        FROM exam_request
                        WHERE id = $1
                        """)
                .execute(Tuple.of(id))
                .map(this::mapOptional);
    }

    public Uni<List<ExamRequest>> findByClinicId(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, patient_id, exam_type, priority,
                               diagnostic_hypothesis, clinical_history, additional_notes,
                               status, requested_by, created_at, updated_at
                        FROM exam_request
                        WHERE clinic_id = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(this::mapList);
    }

    public Uni<List<ExamRequest>> findByPatientId(UUID patientId) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, patient_id, exam_type, priority,
                               diagnostic_hypothesis, clinical_history, additional_notes,
                               status, requested_by, created_at, updated_at
                        FROM exam_request
                        WHERE patient_id = $1
                        ORDER BY created_at DESC
                        """)
                .execute(Tuple.of(patientId))
                .map(this::mapList);
    }

    public Uni<Long> countByClinicId(UUID clinicId) {
        return client.preparedQuery("SELECT count(*) AS total FROM exam_request WHERE clinic_id = $1")
                .execute(Tuple.of(clinicId))
                .map(rows -> rows.iterator().next().getLong("total"));
    }

    public Uni<List<ExamRequest>> findByStatus(ExamRequestStatus status, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT id, clinic_id, patient_id, exam_type, priority,
                               diagnostic_hypothesis, clinical_history, additional_notes,
                               status, requested_by, created_at, updated_at
                        FROM exam_request
                        WHERE status = $1
                        ORDER BY created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(status.name(), limit, offset))
                .map(this::mapList);
    }

    public Uni<ExamRequest> update(ExamRequest examRequest) {
        Tuple params = Tuple.tuple();
        params.addUUID(examRequest.id());
        params.addString(examRequest.examType());
        params.addString(examRequest.priority().name());
        params.addString(examRequest.diagnosticHypothesis());
        params.addString(examRequest.clinicalHistory());
        params.addString(examRequest.additionalNotes());
        params.addString(examRequest.status().name());
        params.addOffsetDateTime(OffsetDateTime.ofInstant(examRequest.updatedAt(), ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE exam_request
                        SET exam_type = $2, priority = $3, diagnostic_hypothesis = $4,
                            clinical_history = $5, additional_notes = $6, status = $7, updated_at = $8
                        WHERE id = $1
                        RETURNING id, clinic_id, patient_id, exam_type, priority,
                                  diagnostic_hypothesis, clinical_history, additional_notes,
                                  status, requested_by, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<ExamRequest> updateStatus(UUID id, ExamRequestStatus status) {
        Tuple params = Tuple.tuple();
        params.addUUID(id);
        params.addString(status.name());
        params.addOffsetDateTime(OffsetDateTime.now(ZoneOffset.UTC));

        return client.preparedQuery("""
                        UPDATE exam_request
                        SET status = $2, updated_at = $3
                        WHERE id = $1
                        RETURNING id, clinic_id, patient_id, exam_type, priority,
                                  diagnostic_hypothesis, clinical_history, additional_notes,
                                  status, requested_by, created_at, updated_at
                        """)
                .execute(params)
                .map(rows -> mapRow(rows.iterator().next()));
    }

    public Uni<List<ExamRequestWithAppointmentResponse>> findByClinicIdWithAppointment(UUID clinicId, int offset, int limit) {
        return client.preparedQuery("""
                        SELECT er.id, er.clinic_id, er.patient_id, er.exam_type, er.priority,
                               er.diagnostic_hypothesis, er.clinical_history, er.additional_notes,
                               er.status, er.requested_by, er.created_at, er.updated_at,
                               a.id AS appointment_id, a.status AS appointment_status
                        FROM exam_request er
                        LEFT JOIN LATERAL (
                            SELECT ap.id, ap.status
                            FROM appointment ap
                            WHERE ap.exam_request_id = er.id
                              AND ap.status NOT IN ('CANCELLED', 'NO_SHOW')
                            ORDER BY ap.created_at DESC
                            LIMIT 1
                        ) a ON true
                        WHERE er.clinic_id = $1
                        ORDER BY er.created_at DESC
                        LIMIT $2 OFFSET $3
                        """)
                .execute(Tuple.of(clinicId, limit, offset))
                .map(rows -> {
                    List<ExamRequestWithAppointmentResponse> result = new ArrayList<>();
                    for (Row row : rows) {
                        result.add(mapRowWithAppointment(row));
                    }
                    return result;
                });
    }

    // ---- Row Mapping ----

    private ExamRequest mapRow(Row row) {
        return ExamRequest.restore(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getUUID("patient_id"),
                row.getString("exam_type"),
                ExamPriority.valueOf(row.getString("priority")),
                row.getString("diagnostic_hypothesis"),
                row.getString("clinical_history"),
                row.getString("additional_notes"),
                ExamRequestStatus.valueOf(row.getString("status")),
                row.getString("requested_by"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant()
        );
    }

    private ExamRequestWithAppointmentResponse mapRowWithAppointment(Row row) {
        UUID appointmentId = null;
        String appointmentStatus = null;
        try {
            appointmentId = row.getUUID("appointment_id");
        } catch (Exception ignored) {
        }
        try {
            appointmentStatus = row.getString("appointment_status");
        } catch (Exception ignored) {
        }
        return new ExamRequestWithAppointmentResponse(
                row.getUUID("id"),
                row.getUUID("clinic_id"),
                row.getUUID("patient_id"),
                row.getString("exam_type"),
                row.getString("priority"),
                row.getString("diagnostic_hypothesis"),
                row.getString("clinical_history"),
                row.getString("additional_notes"),
                row.getString("status"),
                row.getString("requested_by"),
                row.getOffsetDateTime("created_at").toInstant(),
                row.getOffsetDateTime("updated_at").toInstant(),
                appointmentId,
                appointmentStatus
        );
    }

    private Optional<ExamRequest> mapOptional(RowSet<Row> rows) {
        if (rows.rowCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(mapRow(rows.iterator().next()));
    }

    private List<ExamRequest> mapList(RowSet<Row> rows) {
        List<ExamRequest> examRequests = new ArrayList<>();
        for (Row row : rows) {
            examRequests.add(mapRow(row));
        }
        return examRequests;
    }
}
