package dev.vetra.api.modules.patient.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class PatientResourceTest {

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic Patient %s",
                    "document": "PAT%s",
                    "email": "clinic.pat%s@test.com",
                    "phone": "1199990000",
                    "city": "SP",
                    "state": "SP"
                }
                """.formatted(suffix, suffix, suffix);
        String clinicId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Approve the clinic so it becomes ACTIVE
        given()
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", clinicId)
                .then()
                .statusCode(200);

        return clinicId;
    }

    private String createTutor(String clinicId, String suffix) {
        String body = """
                {
                    "name": "Tutor Patient %s",
                    "email": "tutor.pat%s@test.com",
                    "phone": "16999000000",
                    "document": "PAT_DOC_%s"
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
                    "name": "Patient %s",
                    "species": "DOG",
                    "breed": "Mixed",
                    "sex": "MALE"
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
    void shouldCreatePatientAndReturn201() {
        String clinicId = createClinic("pat01");
        String tutorId = createTutor(clinicId, "pat01");

        String body = """
                {
                    "name": "Rex",
                    "species": "DOG",
                    "breed": "Labrador",
                    "sex": "MALE",
                    "birthDate": "2020-03-15",
                    "weightKg": 32.5
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("clinicId", equalTo(clinicId))
                .body("tutorId", equalTo(tutorId))
                .body("name", equalTo("Rex"))
                .body("species", equalTo("DOG"))
                .body("breed", equalTo("Labrador"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn400WhenNameMissing() {
        String clinicId = createClinic("pat02");
        String tutorId = createTutor(clinicId, "pat02");

        String body = """
                {
                    "species": "CAT",
                    "breed": "Siamese"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldListPatientsByClinic() {
        String clinicId = createClinic("pat03");
        String tutorId = createTutor(clinicId, "pat03");

        String body = """
                {
                    "name": "Buddy",
                    "species": "DOG",
                    "breed": "Golden",
                    "sex": "MALE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/clinics/{clinicId}/patients", clinicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldGetPatientById() {
        String clinicId = createClinic("pat04");
        String tutorId = createTutor(clinicId, "pat04");

        String body = """
                {
                    "name": "Luna",
                    "species": "CAT",
                    "breed": "Persian",
                    "sex": "FEMALE"
                }
                """;

        String patientId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/patients/{id}", patientId)
                .then()
                .statusCode(200)
                .body("id", equalTo(patientId))
                .body("name", equalTo("Luna"))
                .body("species", equalTo("CAT"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldUpdatePatient() {
        String clinicId = createClinic("pat05");
        String tutorId = createTutor(clinicId, "pat05");

        String createBody = """
                {
                    "name": "Max Original",
                    "species": "DOG",
                    "breed": "Poodle",
                    "sex": "MALE",
                    "weightKg": 8.0
                }
                """;

        String patientId = given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updateBody = """
                {
                    "name": "Max Updated",
                    "species": "DOG",
                    "breed": "Poodle",
                    "sex": "MALE",
                    "weightKg": 9.5
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/patients/{id}", patientId)
                .then()
                .statusCode(200)
                .body("id", equalTo(patientId))
                .body("name", equalTo("Max Updated"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldListPatientsByTutor() {
        String clinicId = createClinic("pat06");
        String tutorId = createTutor(clinicId, "pat06");

        String body = """
                {
                    "name": "Mimi",
                    "species": "CAT",
                    "breed": "Siamese",
                    "sex": "FEMALE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/tutors/{tutorId}/patients", tutorId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenPatientNotFound() {
        given()
                .when()
                .get("/api/v1/patients/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenUpdatingNonExistentPatient() {
        String body = """
                {
                    "name": "Ghost Pet",
                    "species": "DOG",
                    "breed": "Unknown",
                    "sex": "MALE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/patients/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenCreatingPatientForNonExistentTutor() {
        String clinicId = createClinic("pat07");

        String body = """
                {
                    "name": "Orphan Pet",
                    "species": "CAT",
                    "breed": "Unknown",
                    "sex": "FEMALE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldDeletePatientAndReturn204() {
        String clinicId = createClinic("pdel01");
        String tutorId = createTutor(clinicId, "pdel01");
        String patientId = createPatient(clinicId, tutorId, "pdel01");

        given()
                .when()
                .delete("/api/v1/patients/{id}", patientId)
                .then()
                .statusCode(204);

        // Verify patient is gone
        given()
                .when()
                .get("/api/v1/patients/{id}", patientId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenDeletingNonExistentPatient() {
        given()
                .when()
                .delete("/api/v1/patients/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn422WhenCreatingPatientForNonActiveClinic() {
        // Create clinic WITHOUT approving
        String body = """
                {
                    "name": "Clinic Pending PAT",
                    "document": "PENDPAT01",
                    "email": "clinic.pending.pat@test.com",
                    "phone": "1199990000",
                    "city": "SP",
                    "state": "SP"
                }
                """;
        String clinicId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Need a tutor first — but can't create one for non-active clinic.
        // So we verify at the patient creation level by using a valid tutor from another clinic
        // Actually, we can just try to create a patient — the clinic status check happens before tutor check
        String patientBody = """
                {
                    "name": "Should Fail Patient",
                    "species": "DOG"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(patientBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, java.util.UUID.randomUUID())
                .then()
                .statusCode(422);
    }
}
