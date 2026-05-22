package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class OperationsEndpointsTest {
    @Test
    void healthEndpointIsAvailable() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void metricsEndpointIsAvailable() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("http_server_requests"));
    }

    @Test
    void openApiEndpointIsAvailable() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("openapi"));
    }
}
