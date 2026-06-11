package dev.vetra.api.modules.exam.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ExamRequestResourceTest {

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic Exam %s",
                    "document": "EXAM%s",
                    "email": "clinic.exam%s@test.com",
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
                    "name": "Tutor Exam %s",
                    "email": "tutor.exam%s@test.com",
                    "phone": "16999000000",
                    "document": "EXAM_DOC_%s"
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
                    "name": "Pet Exam %s",
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

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldCreateExamRequestAndReturn201() {
        String clinicId = createClinic("ex01");
        String tutorId = createTutor(clinicId, "ex01");
        String patientId = createPatient(clinicId, tutorId, "ex01");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "Annual checkup"
                }
                """.formatted(patientId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("clinicId", equalTo(clinicId))
                .body("patientId", equalTo(patientId))
                .body("examType", equalTo("ABDOMINAL_ULTRASOUND"))
                .body("priority", equalTo("ROUTINE"))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn400WhenPatientIdMissing() {
        String clinicId = createClinic("ex02");

        String body = """
                {
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldListExamRequestsByClinic() {
        String clinicId = createClinic("ex03");
        String tutorId = createTutor(clinicId, "ex03");
        String patientId = createPatient(clinicId, tutorId, "ex03");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "Test list"
                }
                """.formatted(patientId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldGetExamRequestById() {
        String clinicId = createClinic("ex04");
        String tutorId = createTutor(clinicId, "ex04");
        String patientId = createPatient(clinicId, tutorId, "ex04");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "URGENT",
                    "clinicalHistory": "Urgent case"
                }
                """.formatted(patientId);

        String examId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/exam-requests/{id}", examId)
                .then()
                .statusCode(200)
                .body("id", equalTo(examId))
                .body("priority", equalTo("URGENT"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldCancelExamRequest() {
        String clinicId = createClinic("ex05");
        String tutorId = createTutor(clinicId, "ex05");
        String patientId = createPatient(clinicId, tutorId, "ex05");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "To be cancelled"
                }
                """.formatted(patientId);

        String examId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/exam-requests/{id}/cancel", examId)
                .then()
                .statusCode(200)
                .body("id", equalTo(examId))
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldListExamRequestsByPatient() {
        String clinicId = createClinic("ex06");
        String tutorId = createTutor(clinicId, "ex06");
        String patientId = createPatient(clinicId, tutorId, "ex06");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "Patient history list"
                }
                """.formatted(patientId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/patients/{patientId}/exam-requests", patientId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenExamRequestNotFound() {
        given()
                .when()
                .get("/api/v1/exam-requests/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenCancellingNonExistentExamRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/exam-requests/{id}/cancel", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn422WhenCancellingAlreadyCancelledExamRequest() {
        String clinicId = createClinic("ex07");
        String tutorId = createTutor(clinicId, "ex07");
        String patientId = createPatient(clinicId, tutorId, "ex07");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "To be cancelled twice"
                }
                """.formatted(patientId);

        String examId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // First cancel succeeds
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/exam-requests/{id}/cancel", examId)
                .then()
                .statusCode(200);

        // Second cancel should fail - already CANCELLED
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/exam-requests/{id}/cancel", examId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenCreatingExamRequestWithNonExistentPatient() {
        String clinicId = createClinic("ex08");

        String body = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "Non-existent patient"
                }
                """.formatted(java.util.UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldUpdateExamRequest() {
        String clinicId = createClinic("ex09");
        String tutorId = createTutor(clinicId, "ex09");
        String patientId = createPatient(clinicId, tutorId, "ex09");

        String createBody = """
                {
                    "patientId": "%s",
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "ROUTINE",
                    "clinicalHistory": "Original history"
                }
                """.formatted(patientId);

        String examId = given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/exam-requests", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updateBody = """
                {
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "URGENT",
                    "clinicalHistory": "Updated history with new symptoms"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/exam-requests/{id}", examId)
                .then()
                .statusCode(200)
                .body("id", equalTo(examId))
                .body("priority", equalTo("URGENT"))
                .body("clinicalHistory", equalTo("Updated history with new symptoms"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenUpdatingNonExistentExamRequest() {
        String updateBody = """
                {
                    "examType": "ABDOMINAL_ULTRASOUND",
                    "priority": "URGENT",
                    "clinicalHistory": "Updated"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/exam-requests/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
