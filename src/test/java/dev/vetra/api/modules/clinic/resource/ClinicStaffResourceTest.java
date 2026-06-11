package dev.vetra.api.modules.clinic.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ClinicStaffResourceTest {

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic Staff %s",
                    "document": "STAFF%s",
                    "email": "clinic.staff%s@test.com",
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

    private String createStaff(String clinicId, String suffix, String role) {
        String body = """
                {
                    "name": "Staff %s",
                    "email": "staff.%s@test.com",
                    "phone": "16999000000",
                    "role": "%s",
                    "password": "Str0ngP@ss!"
                }
                """.formatted(suffix, suffix, role);
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldCreateStaffAndReturn201() {
        String clinicId = createClinic("st01");

        String body = """
                {
                    "name": "Dr. Ana Souza",
                    "email": "ana.st01@test.com",
                    "phone": "16999001122",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("clinicId", equalTo(clinicId))
                .body("name", equalTo("Dr. Ana Souza"))
                .body("email", equalTo("ana.st01@test.com"))
                .body("role", equalTo("VETERINARIAN"))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldCreateSecretaryAndReturn201() {
        String clinicId = createClinic("st02");

        String body = """
                {
                    "name": "Maria Secretaria",
                    "email": "maria.st02@test.com",
                    "phone": "16999002233",
                    "role": "SECRETARY",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("clinicId", equalTo(clinicId))
                .body("name", equalTo("Maria Secretaria"))
                .body("role", equalTo("SECRETARY"))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldReturn400WhenNameMissing() {
        String clinicId = createClinic("st03");

        String body = """
                {
                    "email": "noname.st03@test.com",
                    "phone": "16999000000",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldReturn409WhenEmailDuplicate() {
        String clinicId = createClinic("st04");

        String body = """
                {
                    "name": "First Staff",
                    "email": "dup.st04@test.com",
                    "phone": "16999004400",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201);

        // Try to create another staff with the same email
        String duplicateBody = """
                {
                    "name": "Second Staff",
                    "email": "dup.st04@test.com",
                    "phone": "16999004411",
                    "role": "SECRETARY",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(duplicateBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenClinicNotFound() {
        String body = """
                {
                    "name": "Orphan Staff",
                    "email": "orphan.st05@test.com",
                    "phone": "16999005500",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldReturn422WhenClinicNotActive() {
        // Create clinic WITHOUT approving (stays PENDING_APPROVAL)
        String clinicBody = """
                {
                    "name": "Clinic Pending Staff",
                    "document": "PENDSTAFF06",
                    "email": "clinic.pending.staff06@test.com",
                    "phone": "1199990000",
                    "city": "SP",
                    "state": "SP"
                }
                """;
        String clinicId = given()
                .contentType(ContentType.JSON)
                .body(clinicBody)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");

        String staffBody = """
                {
                    "name": "Should Fail Staff",
                    "email": "fail.st06@test.com",
                    "phone": "16999006600",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(staffBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldListStaffByClinic() {
        String clinicId = createClinic("st07");

        String body = """
                {
                    "name": "Staff List Test",
                    "email": "staff.list07@test.com",
                    "phone": "16999007700",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0));
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldGetStaffById() {
        String clinicId = createClinic("st08");

        String body = """
                {
                    "name": "Staff GetById",
                    "email": "staff.get08@test.com",
                    "phone": "16999008800",
                    "role": "SECRETARY",
                    "password": "Str0ngP@ss!"
                }
                """;

        String staffId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/staff/{id}", staffId)
                .then()
                .statusCode(200)
                .body("id", equalTo(staffId))
                .body("name", equalTo("Staff GetById"))
                .body("role", equalTo("SECRETARY"));
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldReturn404WhenStaffNotFound() {
        given()
                .when()
                .get("/api/v1/staff/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldUpdateStaff() {
        String clinicId = createClinic("st10");

        String createBody = """
                {
                    "name": "Staff Original",
                    "email": "staff.orig10@test.com",
                    "phone": "16999010000",
                    "role": "VETERINARIAN",
                    "password": "Str0ngP@ss!"
                }
                """;

        String staffId = given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/v1/clinics/{clinicId}/staff", clinicId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updateBody = """
                {
                    "name": "Staff Updated",
                    "phone": "16999010011",
                    "role": "SECRETARY"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/staff/{id}", staffId)
                .then()
                .statusCode(200)
                .body("id", equalTo(staffId))
                .body("name", equalTo("Staff Updated"))
                .body("phone", equalTo("16999010011"))
                .body("role", equalTo("SECRETARY"))
                .body("email", equalTo("staff.orig10@test.com"));
    }

    @Test
    @TestSecurity(user = "clinicadmin", roles = {"CLINIC_ADMIN"})
    void shouldDeactivateStaff() {
        String clinicId = createClinic("st11");
        String staffId = createStaff(clinicId, "deact11", "VETERINARIAN");

        given()
                .when()
                .delete("/api/v1/staff/{id}", staffId)
                .then()
                .statusCode(200)
                .body("id", equalTo(staffId))
                .body("status", equalTo("INACTIVE"));
    }
}
