package dev.vetra.api.modules.specialist.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class SpecialistResourceTest {

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldCreateSpecialistAndReturn201() {
        String body = """
                {
                    "name": "Dr. Test Create",
                    "email": "drtest.create@vet.com",
                    "phone": "11999887766",
                    "crmv": "10001",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Sao Paulo",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test bio for creation"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Dr. Test Create"))
                .body("email", equalTo("drtest.create@vet.com"))
                .body("crmv", equalTo("10001"))
                .body("crmvState", equalTo("SP"))
                .body("specialty", equalTo("ABDOMINAL_ULTRASOUND"))
                .body("baseCity", equalTo("Sao Paulo"))
                .body("baseState", equalTo("SP"))
                .body("maxTravelRadiusKm", equalTo(50))
                .body("hasOwnEquipment", equalTo(true))
                .body("status", equalTo("PENDING_APPROVAL"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn400WhenNameMissing() {
        String body = """
                {
                    "email": "noname@vet.com",
                    "crmv": "10002",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "hasOwnEquipment": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldListSpecialists() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/specialists")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(10))
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldGetSpecialistById() {
        String body = """
                {
                    "name": "Dr. Test GetById",
                    "email": "drtest.getbyid@vet.com",
                    "phone": "11999887700",
                    "crmv": "10003",
                    "crmvState": "RJ",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Rio de Janeiro",
                    "baseState": "RJ",
                    "maxTravelRadiusKm": 30,
                    "hasOwnEquipment": false,
                    "bio": "Test bio for get by id"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/specialists/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Dr. Test GetById"))
                .body("email", equalTo("drtest.getbyid@vet.com"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldUpdateSpecialist() {
        String createBody = """
                {
                    "name": "Dr. Test Update",
                    "email": "drtest.update@vet.com",
                    "phone": "11999887711",
                    "crmv": "10004",
                    "crmvState": "MG",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Belo Horizonte",
                    "baseState": "MG",
                    "maxTravelRadiusKm": 40,
                    "hasOwnEquipment": true,
                    "bio": "Original bio"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        String updateBody = """
                {
                    "name": "Dr. Test Updated",
                    "email": "drtest.updated@vet.com",
                    "phone": "11999887722",
                    "crmv": "10004",
                    "crmvState": "MG",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Uberlandia",
                    "baseState": "MG",
                    "maxTravelRadiusKm": 60,
                    "hasOwnEquipment": false,
                    "bio": "Updated bio"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/v1/specialists/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Dr. Test Updated"))
                .body("email", equalTo("drtest.updated@vet.com"))
                .body("baseCity", equalTo("Uberlandia"))
                .body("maxTravelRadiusKm", equalTo(60))
                .body("hasOwnEquipment", equalTo(false))
                .body("bio", equalTo("Updated bio"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldApproveSpecialist() {
        String body = """
                {
                    "name": "Dr. Test Approve",
                    "email": "drtest.approve@vet.com",
                    "phone": "11999887733",
                    "crmv": "10005",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Sao Paulo",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test bio for approval"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .body("status", equalTo("PENDING_APPROVAL"))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/specialists/{id}/approve", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldAddCoverageArea() {
        String specialistBody = """
                {
                    "name": "Dr. Test AddArea",
                    "email": "drtest.addarea@vet.com",
                    "phone": "11999887744",
                    "crmv": "10006",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Sao Paulo",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test bio for add area"
                }
                """;

        String specialistId = given()
                .contentType(ContentType.JSON)
                .body(specialistBody)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        String areaBody = """
                {
                    "city": "Campinas",
                    "state": "SP",
                    "radiusKm": 30
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(areaBody)
                .when()
                .post("/api/v1/specialists/{id}/coverage-areas", specialistId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("specialistId", equalTo(specialistId))
                .body("city", equalTo("Campinas"))
                .body("state", equalTo("SP"))
                .body("radiusKm", equalTo(30));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldListCoverageAreas() {
        String specialistBody = """
                {
                    "name": "Dr. Test ListAreas",
                    "email": "drtest.listareas@vet.com",
                    "phone": "11999887755",
                    "crmv": "10007",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Sao Paulo",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test bio for list areas"
                }
                """;

        String specialistId = given()
                .contentType(ContentType.JSON)
                .body(specialistBody)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        String areaBody = """
                {
                    "city": "Santos",
                    "state": "SP",
                    "radiusKm": 20
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(areaBody)
                .when()
                .post("/api/v1/specialists/{id}/coverage-areas", specialistId)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/api/v1/specialists/{id}/coverage-areas", specialistId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldRemoveCoverageArea() {
        String specialistBody = """
                {
                    "name": "Dr. Test RemoveArea",
                    "email": "drtest.removearea@vet.com",
                    "phone": "11999887788",
                    "crmv": "10008",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "Sao Paulo",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test bio for remove area"
                }
                """;

        String specialistId = given()
                .contentType(ContentType.JSON)
                .body(specialistBody)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        String areaBody = """
                {
                    "city": "Sorocaba",
                    "state": "SP",
                    "radiusKm": 25
                }
                """;

        String areaId = given()
                .contentType(ContentType.JSON)
                .body(areaBody)
                .when()
                .post("/api/v1/specialists/{id}/coverage-areas", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .delete("/api/v1/specialists/{id}/coverage-areas/{areaId}", specialistId, areaId)
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenSpecialistNotFound() {
        given()
                .when()
                .get("/api/v1/specialists/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenUpdatingNonExistentSpecialist() {
        String body = """
                {
                    "name": "Updated Spec",
                    "email": "upd@vet.com",
                    "phone": "11999000000",
                    "crmv": "99999",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Updated"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/specialists/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn409WhenDuplicateCrmv() {
        String body1 = """
                {
                    "name": "Dr. Dup CRMV 1",
                    "email": "dr.dupcrmv1@vet.com",
                    "phone": "11999880001",
                    "crmv": "DUP999",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "First"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body1)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201);

        String body2 = """
                {
                    "name": "Dr. Dup CRMV 2",
                    "email": "dr.dupcrmv2@vet.com",
                    "phone": "11999880002",
                    "crmv": "DUP999",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Second"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body2)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenApprovingNonExistentSpecialist() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/specialists/{id}/approve", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn422WhenApprovingAlreadyApprovedSpecialist() {
        String body = """
                {
                    "name": "Dr. Double Approve",
                    "email": "dr.doubleapprove@vet.com",
                    "phone": "11999887799",
                    "crmv": "DBLAP1",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test double approve"
                }
                """;

        String id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        // First approve should succeed
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/specialists/{id}/approve", id)
                .then()
                .statusCode(200);

        // Second approve should fail - already ACTIVE
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/specialists/{id}/approve", id)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenAddingCoverageAreaToNonExistentSpecialist() {
        String areaBody = """
                {
                    "city": "Nowhere",
                    "state": "SP",
                    "radiusKm": 30
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(areaBody)
                .when()
                .post("/api/v1/specialists/{id}/coverage-areas", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenRemovingNonExistentCoverageArea() {
        String specialistBody = """
                {
                    "name": "Dr. RemoveArea404",
                    "email": "dr.rmarea404@vet.com",
                    "phone": "11999887701",
                    "crmv": "RM404",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test"
                }
                """;

        String specialistId = given()
                .contentType(ContentType.JSON)
                .body(specialistBody)
                .when()
                .post("/api/v1/specialists")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .delete("/api/v1/specialists/{id}/coverage-areas/{areaId}", specialistId, java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldSearchSpecialists() {
        given()
                .queryParam("examType", "ABDOMINAL_ULTRASOUND")
                .queryParam("city", "SP")
                .queryParam("state", "SP")
                .when()
                .get("/api/v1/specialists/search")
                .then()
                .statusCode(200);
    }
}
