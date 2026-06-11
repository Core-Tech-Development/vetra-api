package dev.vetra.api.modules.audit.resource;

import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuditResourceTest {

    @Inject
    LogAuditEventUseCase logAuditEventUseCase;

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldListAuditLogs() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/admin/audit-logs")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(10))
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldListAuditLogsWithDifferentPageSize() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/admin/audit-logs")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(5))
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"PLATFORM_ADMIN"})
    void shouldListAuditLogsWithData() {
        // Create audit events to exercise AuditLogRepository.save,
        // AuditLog domain, and AuditLogMapper
        logAuditEventUseCase.execute(
                "admin",
                "clinic",
                UUID.randomUUID(),
                "CLINIC_APPROVED",
                null,
                "{\"status\":\"APPROVED\"}"
        ).await().indefinitely();

        logAuditEventUseCase.execute(
                "admin",
                "specialist",
                UUID.randomUUID(),
                "SPECIALIST_REGISTERED",
                null,
                "{\"status\":\"ACTIVE\"}"
        ).await().indefinitely();

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/admin/audit-logs")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }
}
