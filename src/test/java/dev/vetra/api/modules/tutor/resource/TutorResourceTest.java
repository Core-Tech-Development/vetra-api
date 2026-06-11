package dev.vetra.api.modules.tutor.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class TutorResourceTest {

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic Tutor %s",
                    "document": "TUT%s",
                    "email": "clinic.tutor%s@test.com",
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
                    "name": "Tutor %s",
                    "email": "tutor.%s@test.com",
                    "phone": "16999000000",
                    "document": "TUT_DOC_%s"
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

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldCreateTutorAndReturn201() {
        String clinicId = createClinic("tut01");

        String body = """
                {
                    "name": "Maria Santos",
                    "email": "maria.tut01@test.com",
                    "phone": "16999001122",
                    "document": "11111111101"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("clinicId", equalTo(clinicId))
                .body("name", equalTo("Maria Santos"))
                .body("email", equalTo("maria.tut01@test.com"))
                .body("document", equalTo("11111111101"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn400WhenNameMissing() {
        String clinicId = createClinic("tut02");

        String body = """
                {
                    "email": "noname@test.com",
                    "phone": "16999000000"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldListTutorsByClinic() {
        String clinicId = createClinic("tut03");

        String body = """
                {
                    "name": "Tutor List Test",
                    "email": "tutor.list03@test.com",
                    "phone": "16999003300",
                    "document": "33333333303"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldGetTutorById() {
        String clinicId = createClinic("tut04");

        String body = """
                {
                    "name": "Tutor GetById",
                    "email": "tutor.get04@test.com",
                    "phone": "16999004400",
                    "document": "44444444404"
                }
                """;

        String tutorId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/tutors/{id}", tutorId)
                .then()
                .statusCode(200)
                .body("id", equalTo(tutorId))
                .body("name", equalTo("Tutor GetById"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldUpdateTutor() {
        String clinicId = createClinic("tut05");

        String createBody = """
                {
                    "name": "Tutor Original",
                    "email": "tutor.orig05@test.com",
                    "phone": "16999005500",
                    "document": "55555555505"
                }
                """;

        String tutorId = given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updateBody = """
                {
                    "name": "Tutor Updated",
                    "email": "tutor.upd05@test.com",
                    "phone": "16999005511",
                    "document": "55555555505"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/tutors/{id}", tutorId)
                .then()
                .statusCode(200)
                .body("id", equalTo(tutorId))
                .body("name", equalTo("Tutor Updated"))
                .body("email", equalTo("tutor.upd05@test.com"));
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenTutorNotFound() {
        given()
                .when()
                .get("/api/v1/tutors/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenUpdatingNonExistentTutor() {
        String body = """
                {
                    "name": "Ghost Tutor",
                    "email": "ghost@test.com",
                    "phone": "16999009900"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/tutors/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenCreatingTutorForNonExistentClinic() {
        String body = """
                {
                    "name": "Orphan Tutor",
                    "email": "orphan@test.com",
                    "phone": "16999009911"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldDeleteTutorAndReturn204() {
        String clinicId = createClinic("del01");
        String tutorId = createTutor(clinicId, "del01");

        given()
                .when()
                .delete("/api/v1/tutors/{id}", tutorId)
                .then()
                .statusCode(204);

        // Verify tutor is gone
        given()
                .when()
                .get("/api/v1/tutors/{id}", tutorId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenDeletingNonExistentTutor() {
        given()
                .when()
                .delete("/api/v1/tutors/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn422WhenDeletingTutorWithPatients() {
        String clinicId = createClinic("del02");
        String tutorId = createTutor(clinicId, "del02");

        // Create a patient for this tutor
        String patientBody = """
                {
                    "name": "Pet of tutor",
                    "species": "DOG",
                    "breed": "Labrador"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(patientBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors/{tutorId}/patients", clinicId, tutorId)
                .then()
                .statusCode(201);

        // Try to delete tutor with linked patient — should fail
        given()
                .when()
                .delete("/api/v1/tutors/{id}", tutorId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "clinicuser", roles = {"CLINIC_ADMIN"})
    void shouldReturn422WhenCreatingTutorForNonActiveClinic() {
        // Create clinic WITHOUT approving (stays PENDING_APPROVAL)
        String body = """
                {
                    "name": "Clinic Pending TUT",
                    "document": "PENDTUT01",
                    "email": "clinic.pending.tut@test.com",
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

        String tutorBody = """
                {
                    "name": "Should Fail Tutor",
                    "email": "fail.tut@test.com",
                    "phone": "16999000000"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(tutorBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/tutors", clinicId)
                .then()
                .statusCode(422);
    }
}
