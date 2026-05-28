package com.example;

import com.example.support.GatewayDownstreamMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                        "/api/notifications",
                        "/api/reports"
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

    @Test
    void blocksInternalNotificationEventEndpoints() {
        given()
                .contentType("application/json")
                .body("{\"tenantId\":\"tenant-a\"}")
                .when().post("/api/notifications/events/new-sale")
                .then()
                .statusCode(403)
                .body("message", containsString("gateway_route_forbidden"));
    }

    @Test
    void forwardsNotificationAliasRequestsToNotificationService() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/notification/preferences")
                .then()
                .statusCode(200)
                .body("method", is("GET"))
                .body("path", is("/notifications/preferences"));
    }

    @Test
    void forwardsReportRequestsToReportingService() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/reports/tenants/tenant-a/summary")
                .then()
                .statusCode(200)
                .body("method", is("GET"))
                .body("path", is("/reports/tenants/tenant-a/summary"));
    }

    @Test
    void forwardsReportingAliasRequestsToReportingService() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/reporting/tenants/tenant-a/summary")
                .then()
                .statusCode(200)
                .body("method", is("GET"))
                .body("path", is("/reports/tenants/tenant-a/summary"));
    }

    @Test
    void forwardsBillingWebhookTokenToBillingService() {
        given()
                .contentType("application/json")
                .header("X-Billing-Webhook-Token", "test-webhook-token")
                .body("{\"provider_event_id\":\"evt-1\"}")
                .when().post("/api/billing/webhooks")
                .then()
                .statusCode(200)
                .body("method", is("POST"))
                .body("path", is("/billing/webhooks"))
                .body("billing_webhook_token", is("test-webhook-token"));
    }

    @Test
    void forwardsBinaryReportExportsWithoutChangingBody() {
        byte[] body = given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/reports/tenants/tenant-a/exports/monthly?month=2026-05&format=pdf")
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/pdf"))
                .header("Content-Disposition", containsString("relatorio.pdf"))
                .extract().asByteArray();

        assertTrue(new String(body, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
    }

    @Test
    void blocksInternalReportingEndpoints() {
        given()
                .contentType("application/json")
                .body("{\"tenant_id\":\"tenant-a\"}")
                .when().post("/api/reports/internal/entries")
                .then()
                .statusCode(403)
                .body("message", containsString("gateway_route_forbidden"));
    }
}
