package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/users")
                .then()
                .statusCode(200)
                .body(is("User Service is running"));
    }

    @Test
    void registersTenantWithAdminUser() {
        String email = "owner-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Test LTDA",
                          "tradeName": "Brasaller Test",
                          "adminName": "Owner Test",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201)
                .body("tenant.status", is("ACTIVE"))
                .body("adminUser.email", is(email))
                .body("adminUser.roles.size()", is(2));
    }

    @Test
    void verifiesPasswordOnlyWithInternalToken() {
        String email = "verify-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "legalName": "Brasaller Verify LTDA",
                          "tradeName": "Brasaller Verify",
                          "adminName": "Verify Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/tenants/register")
                .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/verify-password")
                .then()
                .statusCode(403)
                .body("message", is("invalid_internal_token"));

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/users/internal/identity/verify-password")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("roles.size()", is(2));
    }

}
