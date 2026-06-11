package dev.vetra.api.modules.imaging.resource;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ExamFileResourceTest {

    @Inject
    ExamFileRepository examFileRepository;

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic File %s",
                    "document": "FILE%s",
                    "email": "clinic.file%s@test.com",
                    "phone": "1199990000",
                    "city": "SP",
                    "state": "SP"
                }
                """.formatted(suffix, suffix, suffix);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createTutor(String clinicId, String suffix) {
        String body = """
                {
                    "name": "Tutor File %s",
                    "email": "tutor.file%s@test.com",
                    "phone": "16999000000",
                    "document": "FILE_DOC_%s"
                }
                """.formatted(suffix, suffix, suffix);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createPatient(String clinicId, String tutorId, String suffix) {
        String body = """
                {
                    "name": "Pet File %s",
                    "species": "DOG",
                    "breed": "Labrador",
                    "sex": "MALE",
                    "birthDate": "2020-01-01",
                    "weightKg": 25.0
                }
                """.formatted(suffix);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createExamRequest(String clinicId, String patientId, String suffix) {
        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "History %s"
                }
                """.formatted(patientId, suffix);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createSpecialist(String suffix) {
        String body = """
                {
                    "name": "Dr. File %s",
                    "email": "dr.file%s@vet.com",
                    "phone": "11999880000",
                    "crmv": "FILE%s",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test"
                }
                """.formatted(suffix, suffix, suffix);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String createAppointment(String examRequestId, String specialistId) {
        String body = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s"
                }
                """.formatted(examRequestId, specialistId);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "SPECIALIST", "CLINIC_ADMIN"})
    void shouldListExamFilesEmpty() {
        // Use a real appointment to avoid FK issues
        String clinicId = createClinic("file01");
        String tutorId = createTutor(clinicId, "file01");
        String patientId = createPatient(clinicId, tutorId, "file01");
        String examRequestId = createExamRequest(clinicId, patientId, "file01");
        String specialistId = createSpecialist("file01");
        String appointmentId = createAppointment(examRequestId, specialistId);

        given()
                .when()
                .get("/api/v1/appointments/{appointmentId}/files", appointmentId)
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "SPECIALIST", "CLINIC_ADMIN"})
    void shouldListExamFilesWithData() {
        // Create a real appointment for the FK constraint
        String clinicId = createClinic("file02");
        String tutorId = createTutor(clinicId, "file02");
        String patientId = createPatient(clinicId, tutorId, "file02");
        String examRequestId = createExamRequest(clinicId, patientId, "file02");
        String specialistId = createSpecialist("file02");
        String appointmentId = createAppointment(examRequestId, specialistId);
        UUID appointmentUuid = UUID.fromString(appointmentId);

        // Insert exam file records directly (bypass MinIO) to exercise
        // ExamFileRepository.save, ExamFile domain, and ExamFileMapper
        ExamFile file1 = ExamFile.create(
                appointmentUuid,
                "test-image.jpg",
                "IMAGE",
                "image/jpeg",
                "storage/test-image.jpg",
                1024L,
                "testuser"
        );
        examFileRepository.save(file1).await().indefinitely();

        ExamFile file2 = ExamFile.create(
                appointmentUuid,
                "test-report.pdf",
                "PDF",
                "application/pdf",
                "storage/test-report.pdf",
                2048L,
                "testuser"
        );
        examFileRepository.save(file2).await().indefinitely();

        given()
                .when()
                .get("/api/v1/appointments/{appointmentId}/files", appointmentId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "SPECIALIST"})
    void shouldReturn404WhenGettingDownloadUrlForNonExistentFile() {
        given()
                .when()
                .get("/api/v1/files/{id}/download-url", UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "SPECIALIST"})
    void shouldReturn404WhenDeletingNonExistentFile() {
        given()
                .when()
                .delete("/api/v1/files/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
