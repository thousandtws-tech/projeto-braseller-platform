package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void statusEndpointIsAvailable() {
        given()
                .when().get("/notifications")
                .then()
                .statusCode(200)
                .body(is("Notification Service is running"));
    }

    @Test
    void createsDefaultPreferencesForTenant() {
        given()
                .header("Authorization", "Bearer " + token("tenant-a", "user-a", "seller@brasaller.test", "VENDEDOR"))
                .when().get("/notifications/tenants/tenant-a/preferences")
                .then()
                .statusCode(200)
                .body("tenantId", is("tenant-a"))
                .body("emailEnabled", is(true))
                .body("newSaleEnabled", is(false));
    }

    @Test
    void createsNewSaleNotificationWhenEnabled() {
        given()
                .header("Authorization", "Bearer " + token("tenant-sale", "user-sale", "seller@brasaller.test", "ADMIN", "VENDEDOR"))
                .contentType("application/json")
                .body("""
                        {
                          "newSaleEnabled": true,
                          "emailEnabled": true
                        }
                        """)
                .when().put("/notifications/tenants/tenant-sale/preferences")
                .then()
                .statusCode(200)
                .body("newSaleEnabled", is(true));

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "tenantId": "tenant-sale",
                          "recipientEmail": "seller@example.com",
                          "marketplace": "Mercado Livre",
                          "orderId": "ML-123",
                          "amount": 199.90
                        }
                        """)
                .when().post("/notifications/events/new-sale")
                .then()
                .statusCode(201)
                .body("tenantId", is("tenant-sale"))
                .body("type", is("NEW_SALE"))
                .body("message", containsString("ML-123"));

        given()
                .header("Authorization", "Bearer " + token("tenant-sale", "user-sale", "seller@brasaller.test", "VENDEDOR"))
                .when().get("/notifications/tenants/tenant-sale")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void skipsNewSaleNotificationWhenDisabled() {
        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "tenantId": "tenant-disabled",
                          "recipientEmail": "seller@example.com",
                          "marketplace": "Mercado Livre",
                          "orderId": "ML-999",
                          "amount": 50.00
                        }
                        """)
                .when().post("/notifications/events/new-sale")
                .then()
                .statusCode(202)
                .body("reason", is("notification_disabled"));
    }

    @Test
    void sendsWeeklyAccountantReportUsingConfiguredAccountantEmail() {
        given()
                .header("Authorization", "Bearer " + token("tenant-accountant", "user-accountant", "seller@brasaller.test", "ADMIN", "VENDEDOR"))
                .contentType("application/json")
                .body("""
                        {
                          "accountantEmail": "contador@example.com",
                          "weeklyAccountantReportEnabled": true
                        }
                        """)
                .when().put("/notifications/tenants/tenant-accountant/preferences")
                .then()
                .statusCode(200);

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .contentType("application/json")
                .body("""
                        {
                          "tenantId": "tenant-accountant",
                          "weekStart": "2026-05-11",
                          "weekEnd": "2026-05-17",
                          "totalSales": 12,
                          "grossRevenue": 1234.56
                        }
                        """)
                .when().post("/notifications/events/weekly-accountant-report")
                .then()
                .statusCode(201)
                .body("recipientEmail", is("contador@example.com"))
                .body("type", is("WEEKLY_ACCOUNTANT_REPORT"));
    }

    @Test
    void tenantEndpointsRequireJwt() {
        given()
                .when().get("/notifications/tenants/tenant-a/preferences")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void rejectsCrossTenantToken() {
        given()
                .header("Authorization", "Bearer " + token("tenant-b", "user-b", "seller-b@brasaller.test", "VENDEDOR"))
                .when().get("/notifications/tenants/tenant-a/preferences")
                .then()
                .statusCode(403)
                .body("message", is("tenant_mismatch"));
    }

    @Test
    void accountantCanReadButCannotUpdatePreferences() {
        String accountantToken = token("tenant-readonly", "accountant-1", "contador@brasaller.test", "CONTADOR");

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .when().get("/notifications/tenants/tenant-readonly/preferences")
                .then()
                .statusCode(200)
                .body("tenantId", is("tenant-readonly"));

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body("""
                        {
                          "newSaleEnabled": true
                        }
                        """)
                .when().put("/notifications/tenants/tenant-readonly/preferences")
                .then()
                .statusCode(403)
                .body("message", is("write_role_required"));
    }

    @Test
    void eventEndpointsRequireInternalToken() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "tenantId": "tenant-event",
                          "recipientEmail": "seller@example.com",
                          "marketplace": "Mercado Livre",
                          "orderId": "ML-777",
                          "amount": 50.00
                        }
                        """)
                .when().post("/notifications/events/new-sale")
                .then()
                .statusCode(403)
                .body("message", is("invalid_internal_token"));
    }

    private String token(String tenantId, String userId, String email, String... roles) {
        String header = encode("""
                {"alg":"HS256","typ":"JWT"}
                """);
        long expiration = Instant.now().plusSeconds(300).getEpochSecond();
        String groups = Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", "));
        String payload = encode("""
                {
                  "iss": "brasaller-auth",
                  "aud": "brasaller-platform",
                  "exp": %d,
                  "tenant_id": "%s",
                  "user_id": "%s",
                  "email": "%s",
                  "groups": [%s]
                }
                """.formatted(expiration, tenantId, userId, email, groups));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("dev-only-change-me-please-32-bytes-minimum".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
