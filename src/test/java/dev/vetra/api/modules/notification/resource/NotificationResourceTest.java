package dev.vetra.api.modules.notification.resource;

import dev.vetra.api.modules.notification.domain.NotificationChannel;
import dev.vetra.api.modules.notification.usecase.CreateNotificationUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class NotificationResourceTest {

    @Inject
    CreateNotificationUseCase createNotificationUseCase;

    @Test
    @TestSecurity(user = "notif-user-1", roles = {"PLATFORM_ADMIN"})
    void shouldListNotificationsEmpty() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(10))
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "notif-user-2", roles = {"PLATFORM_ADMIN"})
    void shouldListNotificationsWithData() {
        // Create notifications for this user to exercise NotificationRepository.save,
        // Notification domain, and NotificationMapper
        createNotificationUseCase.execute(
                "notif-user-2",
                NotificationChannel.IN_APP,
                "EXAM_READY",
                "Test Title 1",
                "Test message body 1"
        ).await().indefinitely();

        createNotificationUseCase.execute(
                "notif-user-2",
                NotificationChannel.IN_APP,
                "APPOINTMENT_CONFIRMED",
                "Test Title 2",
                "Test message body 2"
        ).await().indefinitely();

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2))
                .body("totalElements", greaterThanOrEqualTo(2));
    }

    @Test
    @TestSecurity(user = "notif-user-3", roles = {"PLATFORM_ADMIN"})
    void shouldListNotificationsWithDefaultPagination() {
        given()
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }
}
