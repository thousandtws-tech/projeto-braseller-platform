package com.example;

import com.example.support.GatewayDownstreamMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(value = GatewayDownstreamMockResource.class, restrictToAnnotatedClass = true)
class ExampleResourceTest {
    @Test
    void routesEndpointListsConfiguredMicroservices() {
        given()
                .when().get("/api")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("routes.publicPath", hasItems(
                        "/api/auth",
                        "/api/users",
                        "/api/core",
                        "/api/billing",
                        "/api/notifications"
                ));
    }

    @Test
    void forwardsAuthRequestsToAuthService() {
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer test-token")
                .body("{\"email\":\"seller@example.com\",\"password\":\"secret\"}")
                .when().post("/api/auth/login")
                .then()
                .statusCode(200)
                .header("X-Downstream-Service", is("mock"))
                .body("method", is("POST"))
                .body("path", is("/auth/login"))
                .body("authorization", is("Bearer test-token"))
                .body("body", containsString("seller@example.com"));
    }

    @Test
    void forwardsQueryParametersToDownstreamService() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/core/context?expand=roles")
                .then()
                .statusCode(200)
                .body("method", is("GET"))
                .body("path", is("/core/context"))
                .body("query", is("expand=roles"));
    }

    @Test
    void blocksInternalUserServiceEndpoints() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"seller@example.com\",\"password\":\"secret\"}")
                .when().post("/api/users/internal/identity/verify-password")
                .then()
                .statusCode(403)
                .body("message", containsString("gateway_route_forbidden"));
    }
}
