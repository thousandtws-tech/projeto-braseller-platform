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
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void statusEndpointReturnsServiceName() {
        given()
                .when().get("/billing")
                .then()
                .statusCode(200)
                .body(is("Billing Service is running"));
    }

    @Test
    void plansEndpointListsCommercialPlans() {
        given()
                .when().get("/billing/plans")
                .then()
                .statusCode(200)
                .body("code", hasItems("BASIC", "PRO", "AGENCY"))
                .body("find { it.code == 'BASIC' }.trial_days", is(14))
                .body("find { it.code == 'PRO' }.currency", is("BRL"));
    }

    @Test
    void tenantSubscriptionRequiresBearerToken() {
        given()
                .when().get("/billing/tenants/tenant-billing/subscription")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void userStartsTrialWithFourteenDaysFree() {
        String tenantId = "tenant-billing-trial";

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .body("""
                        {"plan_code":"BASIC"}
                        """)
                .when().post("/billing/tenants/{tenantId}/trial", tenantId)
                .then()
                .statusCode(201)
                .body("tenant_id", is(tenantId))
                .body("plan_code", is("BASIC"))
                .body("status", is("TRIALING"))
                .body("access_enabled", is(true))
                .body("provider", is("LOCAL"));
    }

    @Test
    void userCanUpgradeAndDowngradePlan() {
        String tenantId = "tenant-billing-change-plan";
        startTrial(tenantId, "BASIC", "ADMIN");

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token(tenantId, "VENDEDOR"))
                .body("""
                        {"plan_code":"PRO"}
                        """)
                .when().put("/billing/tenants/{tenantId}/subscription/plan", tenantId)
                .then()
                .statusCode(200)
                .body("plan_code", is("PRO"))
                .body("status", is("TRIALING"))
                .body("access_enabled", is(true));

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .body("""
                        {"plan_code":"BASIC"}
                        """)
                .when().put("/billing/tenants/{tenantId}/subscription/plan", tenantId)
                .then()
                .statusCode(200)
                .body("plan_code", is("BASIC"));
    }

    @Test
    void accountantCanReadButCannotChangePlan() {
        String tenantId = "tenant-billing-accountant";
        startTrial(tenantId, "PRO", "ADMIN");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/billing/tenants/{tenantId}/subscription", tenantId)
                .then()
                .statusCode(200)
                .body("plan_code", is("PRO"));

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .body("""
                        {"plan_code":"AGENCY"}
                        """)
                .when().put("/billing/tenants/{tenantId}/subscription/plan", tenantId)
                .then()
                .statusCode(403)
                .body("message", is("write_role_required"));
    }

    @Test
    void webhookSuspendsAndReactivatesSubscription() {
        String tenantId = "tenant-billing-webhook";
        startTrial(tenantId, "PRO", "ADMIN");

        webhook("stripe", "evt-payment-failed-1", "PAYMENT_FAILED", tenantId, "PRO")
                .statusCode(200)
                .body("status", is("SUSPENDED"))
                .body("access_enabled", is(false));

        webhook("stripe", "evt-payment-succeeded-1", "PAYMENT_SUCCEEDED", tenantId, "PRO")
                .statusCode(200)
                .body("status", is("ACTIVE"))
                .body("access_enabled", is(true));
    }

    @Test
    void webhookRequiresBillingToken() {
        given()
                .contentType("application/json")
                .body(webhookPayload("stripe", "evt-invalid", "PAYMENT_FAILED", "tenant-billing-token", "PRO"))
                .when().post("/billing/webhooks")
                .then()
                .statusCode(403)
                .body("message", is("invalid_billing_webhook_token"));
    }

    private void startTrial(String tenantId, String planCode, String role) {
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token(tenantId, role))
                .body("""
                        {"plan_code":"%s"}
                        """.formatted(planCode))
                .when().post("/billing/tenants/{tenantId}/trial", tenantId)
                .then()
                .statusCode(201);
    }

    private io.restassured.response.ValidatableResponse webhook(
            String provider,
            String eventId,
            String eventType,
            String tenantId,
            String planCode) {
        return given()
                .contentType("application/json")
                .header("X-Billing-Webhook-Token", "dev-billing-webhook-token-change-me")
                .body(webhookPayload(provider, eventId, eventType, tenantId, planCode))
                .when().post("/billing/webhooks")
                .then();
    }

    private String webhookPayload(String provider, String eventId, String eventType, String tenantId, String planCode) {
        return """
                {
                  "provider": "%s",
                  "provider_event_id": "%s",
                  "event_type": "%s",
                  "tenant_id": "%s",
                  "plan_code": "%s",
                  "provider_customer_id": "cus_%s",
                  "provider_subscription_id": "sub_%s",
                  "reason": "test event",
                  "payload": "{\\"source\\":\\"test\\"}"
                }
                """.formatted(provider, eventId, eventType, tenantId, planCode, tenantId, tenantId);
    }

    private String token(String tenantId, String... roles) {
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
                  "user_id": "user-123",
                  "email": "seller@brasaller.test",
                  "groups": [%s]
                }
                """.formatted(expiration, tenantId, groups));
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
