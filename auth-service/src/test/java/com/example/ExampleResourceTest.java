package com.example;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(UserServiceMockResource.class)
class ExampleResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/auth")
                .then()
                .statusCode(200)
                .body(is("Auth Service is running"));
    }

    @Test
    void registersAndLogsInWithEmailAndPassword() {
        String email = "auth-" + System.nanoTime() + "@brasaller.test";

        given()
                .contentType("application/json")
                .body("""
                        {
                          "tenantName": "Tenant Auth Test",
                          "fullName": "Auth Owner",
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/auth/register")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("email", is(email))
                .body("roles.size()", is(2));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "ChangeMe123!"
                        }
                        """.formatted(email))
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", is("Bearer"))
                .body("email", is(email));
    }

}
