package dev.vetra.api.modules.scheduling.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AvailabilitySlotResourceTest {

    private String createSpecialist(String suffix) {
        String body = """
                {
                    "name": "Dr. Slot %s",
                    "email": "dr.slot%s@vet.com",
                    "phone": "11999880000",
                    "crmv": "SLT%s",
                    "crmvState": "SP",
                    "specialty": "ABDOMINAL_ULTRASOUND",
                    "baseCity": "SP",
                    "baseState": "SP",
                    "maxTravelRadiusKm": 50,
                    "hasOwnEquipment": true,
                    "bio": "Test specialist for slots"
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

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldCreateSlotAndReturn201() {
        String specialistId = createSpecialist("slot01");

        String body = """
                {
                    "startAt": "2026-07-01T09:00:00Z",
                    "endAt": "2026-07-01T10:00:00Z"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("specialistId", equalTo(specialistId))
                .body("status", equalTo("AVAILABLE"));
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldListSlots() {
        String specialistId = createSpecialist("slot02");

        String body = """
                {
                    "startAt": "2026-07-02T09:00:00Z",
                    "endAt": "2026-07-02T10:00:00Z"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldDeleteSlot() {
        String specialistId = createSpecialist("slot03");

        String body = """
                {
                    "startAt": "2026-07-03T09:00:00Z",
                    "endAt": "2026-07-03T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .delete("/api/v1/availability-slots/{id}", slotId)
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldBlockSlot() {
        String specialistId = createSpecialist("slot04");

        String body = """
                {
                    "startAt": "2026-07-04T09:00:00Z",
                    "endAt": "2026-07-04T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .body("status", equalTo("AVAILABLE"))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/availability-slots/{id}/block", slotId)
                .then()
                .statusCode(200)
                .body("id", equalTo(slotId))
                .body("status", equalTo("BLOCKED"));
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldReturn404WhenDeletingNonExistentSlot() {
        given()
                .when()
                .delete("/api/v1/availability-slots/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldReturn422WhenDeletingBlockedSlot() {
        String specialistId = createSpecialist("slot05");

        String body = """
                {
                    "startAt": "2026-07-05T09:00:00Z",
                    "endAt": "2026-07-05T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Block the slot first
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/availability-slots/{id}/block", slotId)
                .then()
                .statusCode(200);

        // Try to delete a BLOCKED slot - should fail
        given()
                .when()
                .delete("/api/v1/availability-slots/{id}", slotId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldReturn404WhenBlockingNonExistentSlot() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/availability-slots/{id}/block", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldReturn422WhenBlockingAlreadyBlockedSlot() {
        String specialistId = createSpecialist("slot06");

        String body = """
                {
                    "startAt": "2026-07-06T09:00:00Z",
                    "endAt": "2026-07-06T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Block first time
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/availability-slots/{id}/block", slotId)
                .then()
                .statusCode(200);

        // Block second time - should fail
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/availability-slots/{id}/block", slotId)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "specialist1", roles = {"SPECIALIST", "PLATFORM_ADMIN"})
    void shouldReturn422WhenSlotEndBeforeStart() {
        String specialistId = createSpecialist("slot07");

        String body = """
                {
                    "startAt": "2026-07-07T10:00:00Z",
                    "endAt": "2026-07-07T09:00:00Z"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(422);
    }
}
