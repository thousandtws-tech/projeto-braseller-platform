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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/core")
                .then()
                .statusCode(200)
                .body(is("Core Service is running"));
    }

    @Test
    void contextRequiresBearerToken() {
        given()
                .when().get("/core/context")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void contextReturnsTenantClaimsFromValidToken() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/context")
                .then()
                .statusCode(200)
                .body("tenantId", is("tenant-123"))
                .body("userId", is("user-123"))
                .body("email", is("seller@brasaller.test"))
                .body("readOnly", is(false));
    }

    @Test
    void listsRegisteredConnectorsWithoutMarketplaceCoupling() {
        given()
                .when().get("/core/connectors")
                .then()
                .statusCode(200)
                .body("name", hasItem("sandbox"))
                .body("required_methods.flatten()", hasItem("getOrders"));
    }

    @Test
    void returnsStandardOrderFormatFromConnector() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/orders")
                .then()
                .statusCode(200)
                .body("[0].order_id", is("SANDBOX-1001"))
                .body("[0].platform", is("sandbox"))
                .body("[0].gross_value", is(199.90F))
                .body("[0].platform_fee", is(26.40F))
                .body("[0].net_value", is(173.50F))
                .body("[0].payment_method", is("PIX"))
                .body("[0].status", is("PAID"))
                .body("[0].buyer_name", is("Comprador Sandbox"))
                .body("[0].items[0].sku", is("SKU-001"))
                .body("[0].invoice_number", is("NF-SANDBOX-1001"));
    }

    @Test
    void connectorCanAuthenticateAndReportStatusByName() {
        given()
                .header("Authorization", "Bearer " + token())
                .contentType("application/json")
                .body("""
                        {
                          "credentials": {
                            "code": "oauth-code"
                          }
                        }
                        """)
                .when().post("/core/connectors/sandbox/authenticate")
                .then()
                .statusCode(200)
                .body("platform", is("sandbox"))
                .body("access_token", containsString("sandbox-access-tenant-123"));

        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/status")
                .then()
                .statusCode(200)
                .body("status", is("ACTIVE"));
    }

    @Test
    void unknownConnectorReturnsNotFound() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/ml/orders")
                .then()
                .statusCode(404)
                .body("message", is("connector_not_found: ml"));
    }

    @Test
    void connectorEndpointsRequireBearerToken() {
        given()
                .when().get("/core/connectors/sandbox/orders")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void accountantCanReadButCannotWriteConnectorData() {
        String accountantToken = token("tenant-123", "accountant-123", "contador@brasaller.test", "CONTADOR");

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .when().get("/core/connectors/sandbox/orders")
                .then()
                .statusCode(200)
                .body("[0].order_id", is("SANDBOX-1001"));

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body("""
                        {
                          "credentials": {
                            "code": "oauth-code"
                          }
                        }
                        """)
                .when().post("/core/connectors/sandbox/authenticate")
                .then()
                .statusCode(403)
                .body("message", is("write_role_required"));
    }

    private String token() {
        return token("tenant-123", "user-123", "seller@brasaller.test", "ADMIN", "VENDEDOR");
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
