package dev.vetra.api.modules.scheduling.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AppointmentResourceTest {

    private String createClinic(String suffix) {
        String body = """
                {
                    "name": "Clinic Appt %s",
                    "document": "APT%s",
                    "email": "clinic.apt%s@test.com",
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
                    "name": "Tutor Appt %s",
                    "email": "tutor.apt%s@test.com",
                    "phone": "16999000000",
                    "document": "APT_DOC_%s"
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
                    "name": "Pet Appt %s",
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
                    "name": "Dr. Appt %s",
                    "email": "dr.apt%s@vet.com",
                    "phone": "11999880000",
                    "crmv": "APT%s",
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
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldCreateAppointmentAndReturn201() {
        String clinicId = createClinic("apt01");
        String tutorId = createTutor(clinicId, "apt01");
        String patientId = createPatient(clinicId, tutorId, "apt01");
        String examRequestId = createExamRequest(clinicId, patientId, "apt01");
        String specialistId = createSpecialist("apt01");

        String body = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s"
                }
                """.formatted(examRequestId, specialistId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("examRequestId", equalTo(examRequestId))
                .body("specialistId", equalTo(specialistId))
                .body("status", equalTo("WAITING_SPECIALIST_ACCEPTANCE"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldListAppointments() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/appointments")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldGetAppointmentById() {
        String clinicId = createClinic("apt03");
        String tutorId = createTutor(clinicId, "apt03");
        String patientId = createPatient(clinicId, tutorId, "apt03");
        String examRequestId = createExamRequest(clinicId, patientId, "apt03");
        String specialistId = createSpecialist("apt03");
        String appointmentId = createAppointment(examRequestId, specialistId);

        given()
                .when()
                .get("/api/v1/appointments/{id}", appointmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(appointmentId));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldAcceptAppointment() {
        String clinicId = createClinic("apt04");
        String tutorId = createTutor(clinicId, "apt04");
        String patientId = createPatient(clinicId, tutorId, "apt04");
        String examRequestId = createExamRequest(clinicId, patientId, "apt04");
        String specialistId = createSpecialist("apt04");
        String appointmentId = createAppointment(examRequestId, specialistId);

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACCEPTED"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldProgressThroughTransitAndService() {
        String clinicId = createClinic("apt05");
        String tutorId = createTutor(clinicId, "apt05");
        String patientId = createPatient(clinicId, tutorId, "apt05");
        String examRequestId = createExamRequest(clinicId, patientId, "apt05");
        String specialistId = createSpecialist("apt05");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Accept
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACCEPTED"));

        // Start transit
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/start-transit", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_TRANSIT"));

        // Start service
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/start-service", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_SERVICE"))
                .body("actualStartAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldCompleteExam() {
        String clinicId = createClinic("apt06");
        String tutorId = createTutor(clinicId, "apt06");
        String patientId = createPatient(clinicId, tutorId, "apt06");
        String examRequestId = createExamRequest(clinicId, patientId, "apt06");
        String specialistId = createSpecialist("apt06");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Accept → Transit → Service → Complete
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId).then().statusCode(200);
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/start-transit", appointmentId).then().statusCode(200);
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/start-service", appointmentId).then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/complete-exam", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("EXAM_DONE"))
                .body("actualEndAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldCancelAppointment() {
        String clinicId = createClinic("apt07");
        String tutorId = createTutor(clinicId, "apt07");
        String patientId = createPatient(clinicId, tutorId, "apt07");
        String examRequestId = createExamRequest(clinicId, patientId, "apt07");
        String specialistId = createSpecialist("apt07");
        String appointmentId = createAppointment(examRequestId, specialistId);

        String cancelBody = """
                {
                    "reason": "Test cancellation reason"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(cancelBody)
                .when()
                .patch("/api/v1/appointments/{id}/cancel", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelReason", equalTo("Test cancellation reason"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn404WhenAppointmentNotFound() {
        given()
                .when()
                .get("/api/v1/appointments/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn404WhenAcceptingNonExistentAppointment() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/v1/appointments/{id}/accept", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenAcceptingAlreadyAcceptedAppointment() {
        String clinicId = createClinic("apt08");
        String tutorId = createTutor(clinicId, "apt08");
        String patientId = createPatient(clinicId, tutorId, "apt08");
        String examRequestId = createExamRequest(clinicId, patientId, "apt08");
        String specialistId = createSpecialist("apt08");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Accept first time
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId).then().statusCode(200);

        // Accept second time - already ACCEPTED
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId).then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn404WhenCancellingNonExistentAppointment() {
        String cancelBody = """
                {
                    "reason": "Test"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(cancelBody)
                .when()
                .patch("/api/v1/appointments/{id}/cancel", java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenCancellingAlreadyCancelledAppointment() {
        String clinicId = createClinic("apt09");
        String tutorId = createTutor(clinicId, "apt09");
        String patientId = createPatient(clinicId, tutorId, "apt09");
        String examRequestId = createExamRequest(clinicId, patientId, "apt09");
        String specialistId = createSpecialist("apt09");
        String appointmentId = createAppointment(examRequestId, specialistId);

        String cancelBody = """
                {
                    "reason": "First cancel"
                }
                """;

        // Cancel first time
        given().contentType(ContentType.JSON).body(cancelBody).when()
                .patch("/api/v1/appointments/{id}/cancel", appointmentId).then().statusCode(200);

        // Cancel second time
        given().contentType(ContentType.JSON).body(cancelBody).when()
                .patch("/api/v1/appointments/{id}/cancel", appointmentId).then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenStartingTransitFromWrongStatus() {
        String clinicId = createClinic("apt10");
        String tutorId = createTutor(clinicId, "apt10");
        String patientId = createPatient(clinicId, tutorId, "apt10");
        String examRequestId = createExamRequest(clinicId, patientId, "apt10");
        String specialistId = createSpecialist("apt10");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Try start-transit from WAITING_SPECIALIST_ACCEPTANCE - should fail
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/start-transit", appointmentId).then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenStartingServiceFromWrongStatus() {
        String clinicId = createClinic("apt11");
        String tutorId = createTutor(clinicId, "apt11");
        String patientId = createPatient(clinicId, tutorId, "apt11");
        String examRequestId = createExamRequest(clinicId, patientId, "apt11");
        String specialistId = createSpecialist("apt11");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Accept first
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/accept", appointmentId).then().statusCode(200);

        // Try start-service from ACCEPTED - should fail (needs IN_TRANSIT)
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/start-service", appointmentId).then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenCompletingExamFromWrongStatus() {
        String clinicId = createClinic("apt12");
        String tutorId = createTutor(clinicId, "apt12");
        String patientId = createPatient(clinicId, tutorId, "apt12");
        String examRequestId = createExamRequest(clinicId, patientId, "apt12");
        String specialistId = createSpecialist("apt12");
        String appointmentId = createAppointment(examRequestId, specialistId);

        // Try complete-exam from WAITING_SPECIALIST_ACCEPTANCE - should fail
        given().contentType(ContentType.JSON).when()
                .patch("/api/v1/appointments/{id}/complete-exam", appointmentId).then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldListAppointmentsByStatus() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "WAITING_SPECIALIST_ACCEPTANCE")
                .when()
                .get("/api/v1/appointments")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldListAppointmentsBySpecialist() {
        String clinicId = createClinic("apt13");
        String tutorId = createTutor(clinicId, "apt13");
        String patientId = createPatient(clinicId, tutorId, "apt13");
        String examRequestId = createExamRequest(clinicId, patientId, "apt13");
        String specialistId = createSpecialist("apt13");
        createAppointment(examRequestId, specialistId);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/specialists/{specialistId}/appointments", specialistId)
                .then()
                .statusCode(200)
                .body("content.size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldCreateAppointmentWithAvailabilitySlot() {
        String clinicId = createClinic("apt14");
        String tutorId = createTutor(clinicId, "apt14");
        String patientId = createPatient(clinicId, tutorId, "apt14");
        String examRequestId = createExamRequest(clinicId, patientId, "apt14");
        String specialistId = createSpecialist("apt14");

        // Create an availability slot
        String slotBody = """
                {
                    "startAt": "2026-08-01T09:00:00Z",
                    "endAt": "2026-08-01T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(slotBody)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create appointment with slot
        String body = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s",
                    "availabilitySlotId": "%s"
                }
                """.formatted(examRequestId, specialistId, slotId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("specialistId", equalTo(specialistId));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldCancelAppointmentWithSlotAndFreeSlot() {
        String clinicId = createClinic("apt15");
        String tutorId = createTutor(clinicId, "apt15");
        String patientId = createPatient(clinicId, tutorId, "apt15");
        String examRequestId = createExamRequest(clinicId, patientId, "apt15");
        String specialistId = createSpecialist("apt15");

        // Create an availability slot
        String slotBody = """
                {
                    "startAt": "2026-09-01T09:00:00Z",
                    "endAt": "2026-09-01T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(slotBody)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create appointment with slot
        String body = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s",
                    "availabilitySlotId": "%s"
                }
                """.formatted(examRequestId, specialistId, slotId);

        String appointmentId = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Cancel the appointment - should free the slot
        String cancelBody = """
                {
                    "reason": "Test cancellation to free slot"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(cancelBody)
                .when()
                .patch("/api/v1/appointments/{id}/cancel", appointmentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelReason", equalTo("Test cancellation to free slot"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenSchedulingWithNonExistentSlot() {
        String clinicId = createClinic("apt16");
        String tutorId = createTutor(clinicId, "apt16");
        String patientId = createPatient(clinicId, tutorId, "apt16");
        String examRequestId = createExamRequest(clinicId, patientId, "apt16");
        String specialistId = createSpecialist("apt16");

        String body = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s",
                    "availabilitySlotId": "%s"
                }
                """.formatted(examRequestId, specialistId, java.util.UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN", "CLINIC_ADMIN", "SPECIALIST"})
    void shouldReturn422WhenSchedulingWithAlreadyReservedSlot() {
        String clinicId = createClinic("apt17");
        String tutorId = createTutor(clinicId, "apt17");
        String patientId = createPatient(clinicId, tutorId, "apt17");
        String examRequestId1 = createExamRequest(clinicId, patientId, "apt17a");
        String examRequestId2 = createExamRequest(clinicId, patientId, "apt17b");
        String specialistId = createSpecialist("apt17");

        // Create an availability slot
        String slotBody = """
                {
                    "startAt": "2026-10-01T09:00:00Z",
                    "endAt": "2026-10-01T10:00:00Z"
                }
                """;

        String slotId = given()
                .contentType(ContentType.JSON)
                .body(slotBody)
                .when()
                .post("/api/v1/specialists/{specialistId}/availability-slots", specialistId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // First appointment reserves the slot
        String body1 = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s",
                    "availabilitySlotId": "%s"
                }
                """.formatted(examRequestId1, specialistId, slotId);

        given()
                .contentType(ContentType.JSON)
                .body(body1)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(201);

        // Second appointment tries to use the same slot - should fail
        String body2 = """
                {
                    "examRequestId": "%s",
                    "specialistId": "%s",
                    "availabilitySlotId": "%s"
                }
                """.formatted(examRequestId2, specialistId, slotId);

        given()
                .contentType(ContentType.JSON)
                .body(body2)
                .when()
                .post("/api/v1/appointments")
                .then()
                .statusCode(422);
    }

}
