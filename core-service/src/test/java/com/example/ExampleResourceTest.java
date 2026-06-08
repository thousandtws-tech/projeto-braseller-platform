package com.example;

import io.quarkus.test.junit.QuarkusTest;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import com.example.application.event.ReportEntryUpsertRequestedEvent;
import com.example.application.port.out.MarketplaceConnector;
import com.example.domain.enums.PaymentMethod;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.InvoiceFilters;
import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.StandardOrder;
import com.example.infrastructure.connector.mercadolivre.JdbcMercadoLivreTokenRepository;
import com.example.infrastructure.connector.mercadolivre.MercadoLivreConnectorToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ExampleResourceTest {
    @Inject
    JdbcMercadoLivreTokenRepository mercadoLivreTokenRepository;

    @Inject
    AgroalDataSource dataSource;

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
                .body("name", hasItem("mercado-livre"))
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
                .body("[0].payment_date", containsString("-"))
                .body("[0].release_date", containsString("-"))
                .body("[0].status", is("paid"))
                .body("[0].buyer_name", is("Comprador Sandbox"))
                .body("[0].items[0].sku", is("SKU-001"))
                .body("[0].invoice_number", is("NF-SANDBOX-1001"));
    }

    @Test
    void reportEntryUsesFeeSplitIncludingShippingCost() {
        StandardOrder order = new StandardOrder(
                "ORDER-SPLIT-1001",
                "marketplace",
                LocalDate.of(2026, 5, 21),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                PaymentMethod.PIX,
                LocalDate.of(2026, 5, 21),
                null,
                OrderStatus.PAID,
                "Comprador Teste",
                List.of(),
                "NF-ORDER-SPLIT-1001"
        );

        ReportEntryUpsertRequestedEvent event = ReportEntryUpsertRequestedEvent.fromOrder(
                "tenant-123",
                order,
                List.of(
                        new FeeInfo(order.orderId(), "commission_fee", "Comissao", new BigDecimal("12.00")),
                        new FeeInfo(order.orderId(), "shipping_cost", "Frete", new BigDecimal("8.00"))
                )
        );

        org.hamcrest.MatcherAssert.assertThat(event.grossValue(), is(new BigDecimal("100.00")));
        org.hamcrest.MatcherAssert.assertThat(event.feeValue(), is(new BigDecimal("20.00")));
        org.hamcrest.MatcherAssert.assertThat(event.receivableValue(), is(new BigDecimal("80.00")));
        org.hamcrest.MatcherAssert.assertThat(event.receivedValue(), is(new BigDecimal("0.00")));
    }

    @Test
    void feeInfoStandardizesShippingAliasesAndDebitAmounts() {
        FeeInfo shippingFee = new FeeInfo("ORDER-FEE-1001", "shipping_fee", "Frete", new BigDecimal("-7.505"));
        FeeInfo saleFee = new FeeInfo("ORDER-FEE-1001", "sale_fee", "Comissao", new BigDecimal("12"));

        org.hamcrest.MatcherAssert.assertThat(shippingFee.type(), is(FeeInfo.SHIPPING_COST));
        org.hamcrest.MatcherAssert.assertThat(shippingFee.isShippingCost(), is(true));
        org.hamcrest.MatcherAssert.assertThat(shippingFee.amount(), is(new BigDecimal("7.51")));
        org.hamcrest.MatcherAssert.assertThat(saleFee.type(), is(FeeInfo.COMMISSION_FEE));
        org.hamcrest.MatcherAssert.assertThat(saleFee.amount(), is(new BigDecimal("12.00")));
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
                .body("status", is("active"))
                .body("access_token", nullValue())
                .body("refresh_token", nullValue());

        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/status")
                .then()
                .statusCode(200)
                .body("status", is("active"));
    }

    @Test
    void marketplaceTokensAreEncryptedAtRest() throws SQLException {
        String tenantId = "tenant-token-encryption";
        mercadoLivreTokenRepository.save(new MercadoLivreConnectorToken(
                tenantId,
                "seller-123",
                "ml-access-token-raw",
                "ml-refresh-token-raw",
                Instant.now().plusSeconds(3600)
        ));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT access_token, refresh_token
                     FROM marketplace_connector_tokens
                     WHERE tenant_id = ? AND connector_name = 'mercado-livre'
                     """)) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                String storedAccessToken = resultSet.getString("access_token");
                String storedRefreshToken = resultSet.getString("refresh_token");
                assertNotEquals("ml-access-token-raw", storedAccessToken);
                assertNotEquals("ml-refresh-token-raw", storedRefreshToken);
                assertTrue(storedAccessToken.startsWith("v1:"));
                assertTrue(storedRefreshToken.startsWith("v1:"));
            }
        }

        MercadoLivreConnectorToken decrypted = mercadoLivreTokenRepository.find(tenantId).orElseThrow();
        org.hamcrest.MatcherAssert.assertThat(decrypted.accessToken(), is("ml-access-token-raw"));
        org.hamcrest.MatcherAssert.assertThat(decrypted.refreshToken(), is("ml-refresh-token-raw"));
    }

    @Test
    void mercadoLivreConnectorReportsDisconnectedWhenOAuthIsNotConfigured() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/mercado-livre/status")
                .then()
                .statusCode(200)
                .body("platform", is("mercado-livre"))
                .body("status", is("disconnected"))
                .body("message", is("mercado_livre_oauth_not_configured"));
    }

    @Test
    void acceptsLowercaseOrderStatusFilter() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/orders?status=paid")
                .then()
                .statusCode(200)
                .body("[0].status", is("paid"));
    }

    @Test
    void syncAllIsQueuedForAsyncProcessing() {
        String jobId = given()
                .header("Authorization", "Bearer " + token())
                .contentType("application/json")
                .body("""
                        {
                          "since": "2026-05-01T00:00:00Z"
                        }
                        """)
                .when().post("/core/connectors/sandbox/sync-all")
                .then()
                .statusCode(202)
                .body("status", is("QUEUED"))
                .body("connector_name", is("sandbox"))
                .body("tenant_id", is("tenant-123"))
                .body("job_id", containsString("-"))
                .extract().path("job_id");

        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sync-jobs/{jobId}", jobId)
                .then()
                .statusCode(200)
                .body("job_id", is(jobId))
                .body("status", is("QUEUED"))
                .body("connector_name", is("sandbox"));
    }

    @Test
    void appliesOrderFiltersAndRejectsInvalidStatus() {
        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/orders?status=cancelled")
                .then()
                .statusCode(200)
                .body("size()", is(0));

        given()
                .header("Authorization", "Bearer " + token())
                .when().get("/core/connectors/sandbox/orders?status=unknown")
                .then()
                .statusCode(400)
                .body("message", is("invalid_order_status: unknown"));
    }

    @Test
    void invoicesAreOptionalInConnectorContract() throws Exception {
        boolean defaultMethod = MarketplaceConnector.class
                .getMethod("getInvoices", String.class, InvoiceFilters.class)
                .isDefault();

        org.hamcrest.MatcherAssert.assertThat(defaultMethod, is(true));
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
