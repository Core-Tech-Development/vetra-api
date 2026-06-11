package dev.vetra.api.modules.admin.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AdminResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldReturnAdminDashboard() {
        given()
                .when()
                .get("/api/v1/admin/dashboard")
                .then()
                .statusCode(200)
                .body("totalClinics", notNullValue())
                .body("totalSpecialists", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldApproveClinic() {
        String clinicBody = """
                {
                    "name": "Clinica Admin Approve",
                    "document": "55566677000111",
                    "email": "admin.approve@clinic.com",
                    "phone": "1133334444",
                    "city": "Sao Paulo",
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
                .body("status", equalTo("PENDING_APPROVAL"))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", clinicId)
                .then()
                .statusCode(200)
                .body("id", equalTo(clinicId))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldSuspendClinic() {
        String clinicBody = """
                {
                    "name": "Clinica Admin Suspend",
                    "document": "66677788000122",
                    "email": "admin.suspend@clinic.com",
                    "phone": "1155556666",
                    "city": "Campinas",
                    "state": "SP"
                }
                """;

        // Create the clinic
        String clinicId = given()
                .contentType(ContentType.JSON)
                .body(clinicBody)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Approve the clinic first (must be ACTIVE before suspending)
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", clinicId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Now suspend the clinic
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/suspend", clinicId)
                .then()
                .statusCode(200)
                .body("id", equalTo(clinicId))
                .body("status", equalTo("SUSPENDED"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenApprovingNonExistentClinic() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldReturn422WhenApprovingAlreadyActiveClinic() {
        String clinicBody = """
                {
                    "name": "Clinica Already Active",
                    "document": "77788899000133",
                    "email": "already.active@clinic.com",
                    "phone": "1177778888",
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

        // First approve
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", clinicId)
                .then()
                .statusCode(200);

        // Second approve should fail - already ACTIVE
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/approve", clinicId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenSuspendingNonExistentClinic() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/suspend", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldReturn422WhenSuspendingPendingClinic() {
        String clinicBody = """
                {
                    "name": "Clinica Pending Suspend",
                    "document": "88899900000144",
                    "email": "pending.suspend@clinic.com",
                    "phone": "1188889999",
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
                .body("status", equalTo("PENDING_APPROVAL"))
                .extract().path("id");

        // Try to suspend a PENDING_APPROVAL clinic - should fail
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/admin/clinics/{id}/suspend", clinicId)
                .then()
                .statusCode(422);
    }
}
