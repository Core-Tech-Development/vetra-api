package dev.vetra.api.modules.clinic.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ClinicResourceTest {

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldCreateClinicAndReturn201() {
        String body = """
                {
                    "name": "Clinica Vet Saude",
                    "document": "12345678000199",
                    "email": "contato@vetsaude.com.br",
                    "phone": "1199998888",
                    "address": "Rua das Flores, 100",
                    "city": "Araraquara",
                    "state": "SP"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Clinica Vet Saude"))
                .body("document", equalTo("12345678000199"))
                .body("email", equalTo("contato@vetsaude.com.br"))
                .body("status", equalTo("PENDING_APPROVAL"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn409WhenDuplicateDocument() {
        String body = """
                {
                    "name": "Clinica Duplicada",
                    "document": "99988877000166",
                    "email": "dup@test.com",
                    "phone": "1199990000",
                    "city": "Campinas",
                    "state": "SP"
                }
                """;

        // First creation should succeed
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(201);

        // Second creation with same document should fail
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(409)
                .body("error", equalTo("DUPLICATE"));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn400WhenNameIsMissing() {
        String body = """
                {
                    "document": "11122233000155",
                    "email": "test@test.com"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/clinics")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldListClinicsWithPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/clinics")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(10))
                .body("content", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"PLATFORM_ADMIN"})
    void shouldReturn404WhenClinicNotFound() {
        given()
                .when()
                .get("/api/v1/clinics/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

}
