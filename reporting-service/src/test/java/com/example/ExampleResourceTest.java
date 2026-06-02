package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ExampleResourceTest {
    @Test
    void statusEndpointReturnsServiceName() {
        given()
                .when().get("/reports")
                .then()
                .statusCode(200)
                .body(is("Reporting Service is running"));
    }

    @Test
    void reportEndpointsRequireBearerToken() {
        given()
                .when().get("/reports/tenants/tenant-reporting/summary")
                .then()
                .statusCode(401)
                .body("message", is("missing_bearer_token"));
    }

    @Test
    void internalIngestRequiresInternalToken() {
        given()
                .contentType("application/json")
                .body(entryPayload("tenant-reporting-internal", "SANDBOX-INT", "sandbox", "RECEIVED", "PIX"))
                .when().post("/reports/internal/entries")
                .then()
                .statusCode(403)
                .body("message", is("invalid_internal_token"));
    }

    @Test
    void clicksignWebhookAcceptsValidHmacAndStoresEvent() {
        String payload = clicksignPayload("document_closed", "clicksign-document-123");

        given()
                .contentType("application/json")
                .header("Content-Hmac", contentHmac(payload))
                .body(payload)
                .when().post("/reports/webhooks/clicksign")
                .then()
                .statusCode(200)
                .body("event_name", is("document_closed"))
                .body("account_key", is("clicksign-account-123"))
                .body("envelope_id", is("clicksign-envelope-123"))
                .body("document_key", is("clicksign-document-123"))
                .body("processing_status", is("DOCUMENT_CLOSED"));
    }

    @Test
    void clicksignWebhookRejectsInvalidHmac() {
        String payload = clicksignPayload("sign", "clicksign-document-invalid");

        given()
                .contentType("application/json")
                .header("Content-Hmac", "sha256=invalid")
                .body(payload)
                .when().post("/reports/webhooks/clicksign")
                .then()
                .statusCode(403)
                .body("message", is("clicksign_hmac_invalid"));
    }

    @Test
    void dashboardAggregatesEntriesAcrossMarketplaces() {
        String tenantId = "tenant-reporting-dashboard";
        ingest(tenantId, "SANDBOX-1001", "sandbox", "RECEIVED", "PIX", "199.90", "173.50", "26.40", "0.00");
        ingest(tenantId, "ML-2001", "mercado-livre", "PAID", "CREDIT_CARD", "300.00", "0.00", "45.00", "255.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN", "VENDEDOR"))
                .when().get("/reports/tenants/{tenantId}/dashboard?sort=grossValue&direction=DESC&size=10", tenantId)
                .then()
                .statusCode(200)
                .body("summary.gross_value", is(499.90F))
                .body("summary.received_value", is(173.50F))
                .body("summary.fee_value", is(71.40F))
                .body("summary.receivable_value", is(255.00F))
                .body("summary.entry_count", is(2))
                .body("entries.items[0].order_id", is("ML-2001"))
                .body("filters.platforms", hasItems("sandbox", "mercado-livre"))
                .body("platform_comparison.platform", hasItems("sandbox", "mercado-livre"));
    }

    @Test
    void monthlyExportReturnsCsvWithConsolidatedEntries() {
        String tenantId = "tenant-reporting-export-csv";
        ingest(tenantId, "CSV-1001", "sandbox", "RECEIVED", "PIX", "150.00", "140.00", "10.00", "0.00");
        ingest(tenantId, "CSV-2001", "mercado-livre", "PAID", "CREDIT_CARD", "280.00", "0.00", "42.00", "238.00");

        String csv = given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/exports/monthly?month=2026-05&format=csv", tenantId)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("text/csv"))
                .header("Content-Disposition", containsString(".csv"))
                .extract().asString();

        assertTrue(csv.contains("tenant_id,period,platform,order_id"));
        assertTrue(csv.contains("CSV-1001"));
        assertTrue(csv.contains("CSV-2001"));
    }

    @Test
    void monthlyExportReturnsXlsxWithMarketplaceSheets() throws IOException {
        String tenantId = "tenant-reporting-export-xlsx";
        ingest(tenantId, "XLSX-1001", "sandbox", "RECEIVED", "PIX", "180.00", "170.00", "10.00", "0.00");
        ingest(tenantId, "XLSX-2001", "amazon", "PAID", "BOLETO", "320.00", "0.00", "50.00", "270.00");

        byte[] xlsx = given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/exports/monthly?month=2026-05&format=xlsx", tenantId)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("spreadsheetml.sheet"))
                .header("Content-Disposition", containsString(".xlsx"))
                .extract().asByteArray();

        assertTrue(xlsx.length > 4);
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
        String workbookXml = zipText(xlsx, "xl/workbook.xml");
        assertTrue(workbookXml.contains("Resumo"));
        assertTrue(workbookXml.contains("sandbox"));
        assertTrue(workbookXml.contains("amazon"));
    }

    @Test
    void platformExportReturnsPdfForSingleMarketplace() {
        String tenantId = "tenant-reporting-export-pdf";
        ingest(tenantId, "PDF-1001", "shopee", "RECEIVED", "PIX", "210.00", "200.00", "10.00", "0.00");
        ingest(tenantId, "PDF-2001", "amazon", "PAID", "BOLETO", "410.00", "0.00", "55.00", "355.00");

        byte[] pdf = given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/exports/platforms/shopee?from=2026-05-01&to=2026-05-31&format=pdf", tenantId)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/pdf"))
                .header("Content-Disposition", containsString(".pdf"))
                .extract().asByteArray();

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("PDF-1001"));
    }

    @Test
    void internalAutomationEndpointsReturnSummaryAndPaymentReleases() {
        String tenantId = "tenant-reporting-automation";
        ingest(tenantId, "ML-AUTO-1001", "mercado-livre", "PAID", "PIX", "220.00", "0.00", "20.00", "200.00");
        ingest(tenantId, "SHOP-AUTO-1002", "shopee", "RECEIVED", "PIX", "180.00", "170.00", "10.00", "0.00");

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .when().get("/reports/internal/tenants/{tenantId}/summary?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("gross_value", is(400.00F))
                .body("entry_count", is(2));

        given()
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .when().get("/reports/internal/tenants/{tenantId}/payment-releases?platform=mercado-livre&from=2026-06-01&to=2026-06-10", tenantId)
                .then()
                .statusCode(200)
                .body("[0].payment_id", is("ML-AUTO-1001"))
                .body("[0].amount", is(200.00F))
                .body("[0].release_date", is("2026-06-04"));
    }

    @Test
    void entriesSupportFiltersSearchAndOrdering() {
        String tenantId = "tenant-reporting-search";
        ingest(tenantId, "SHOP-1001", "shopee", "RECEIVED", "PIX", "100.00", "90.00", "10.00", "0.00");
        ingest(tenantId, "AMZ-1002", "amazon", "PENDING_RELEASE", "BOLETO", "250.00", "0.00", "35.00", "215.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/entries?platform=amazon&search=amz&sort=saleDate&direction=ASC", tenantId)
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("items[0].platform", is("amazon"))
                .body("items[0].order_id", is("AMZ-1002"))
                .body("items[0].receivable_value", is(215.00F));
    }

    @Test
    void manualImportAndPublicIntegrationCreateTenantEntries() {
        String tenantId = "tenant-reporting-manual";

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN", "VENDEDOR"))
                .contentType("application/json")
                .body(publicEntryPayload("magalu", "MAG-1001", "RECEIVED", "PIX", "310.00", "290.00", "20.00", "0.00"))
                .when().post("/reports/tenants/{tenantId}/manual-import/entries", tenantId)
                .then()
                .statusCode(201)
                .body("tenant_id", is(tenantId))
                .body("platform", is("magalu"))
                .body("order_id", is("MAG-1001"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body(publicEntryPayload("custom-system", "ERP-2001", "PAID", "BANK_TRANSFER", "500.00", "0.00", "55.00", "445.00"))
                .when().post("/reports/tenants/{tenantId}/integrations/entries", tenantId)
                .then()
                .statusCode(201)
                .body("tenant_id", is(tenantId))
                .body("platform", is("custom-system"))
                .body("order_id", is("ERP-2001"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/entries?size=10", tenantId)
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items.order_id", hasItems("MAG-1001", "ERP-2001"));
    }

    @Test
    void fiscalProfileExpensesAndDreWorkForMvpAccounting() {
        String tenantId = "tenant-reporting-fiscal";
        ingest(tenantId, "DRE-1001", "mercado-livre", "RECEIVED", "PIX", "300.00", "255.00", "45.00", "0.00");
        ingest(tenantId, "DRE-1002", "mercado-livre", "RECEIVED", "CREDIT_CARD", "200.00", "180.00", "20.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(fiscalProfilePayload("LUCRO_PRESUMIDO", "0.1120"))
                .when().put("/reports/tenants/{tenantId}/fiscal-profile", tenantId)
                .then()
                .statusCode(200)
                .body("tax_regime", is("LUCRO_PRESUMIDO"))
                .body("estimated_tax_rate", is(0.1120F));

        String expenseId = given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN", "VENDEDOR"))
                .contentType("application/json")
                .body(expensePayload("PACKAGING", "Embalagens maio", "42.50", true))
                .when().post("/reports/tenants/{tenantId}/expenses", tenantId)
                .then()
                .statusCode(201)
                .body("tenant_id", is(tenantId))
                .body("category", is("PACKAGING"))
                .body("attachment.public_id", is("brasaller/despesas/embalagem-maio"))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(expensePayload("BANK_FEE", "Tarifa bancaria", "17.50", true))
                .when().post("/reports/tenants/{tenantId}/expenses", tenantId)
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(expensePayload("OTHER", "Sem comprovante", "10.00", false))
                .when().post("/reports/tenants/{tenantId}/expenses", tenantId)
                .then()
                .statusCode(400)
                .body("message", is("expense_attachment_required"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/expenses/{expenseId}", tenantId, expenseId)
                .then()
                .statusCode(200)
                .body("amount", is(42.50F))
                .body("attachment.secure_url", is("https://res.cloudinary.com/brasaller/image/upload/v1/despesas/embalagem-maio.pdf"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("tax_regime", is("LUCRO_PRESUMIDO"))
                .body("gross_revenue", is(500.00F))
                .body("marketplace_fees", is(65.00F))
                .body("estimated_taxes", is(56.00F))
                .body("operating_expenses", is(60.00F))
                .body("net_result", is(319.00F))
                .body("expense_count", is(2))
                .body("expenses_by_category.category", hasItems("PACKAGING", "BANK_FEE"));
    }

    @Test
    void dreCalculationCanBeQueuedAndConsulted() {
        String tenantId = "tenant-reporting-dre-async";

        String jobId = given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body("""
                        {
                          "from": "2026-05-01",
                          "to": "2026-05-31"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/dre/jobs", tenantId)
                .then()
                .statusCode(202)
                .body("tenant_id", is(tenantId))
                .body("status", is("QUEUED"))
                .body("from", is("2026-05-01"))
                .body("to", is("2026-05-31"))
                .extract().path("job_id");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre/jobs/{jobId}", tenantId, jobId)
                .then()
                .statusCode(200)
                .body("job_id", is(jobId))
                .body("status", is("QUEUED"));
    }

    @Test
    void expenseUploadSignatureUsesCloudinaryConfiguration() {
        String tenantId = "tenant-reporting-cloudinary";

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/expenses/upload-signature", tenantId)
                .then()
                .statusCode(200)
                .body("cloud_name", is("brasaller-test"))
                .body("api_key", is("test-api-key"))
                .body("upload_url", is("https://api.cloudinary.com/v1_1/brasaller-test/auto/upload"))
                .body("resource_type", is("auto"))
                .body("folder", is("brasaller/despesas/tenant-reporting-cloudinary"))
                .body("use_filename", is(true))
                .body("unique_filename", is(true))
                .body("signature", notNullValue());
    }

    @Test
    void accountantSignatureLocksClosedAccountingPeriod() {
        String tenantId = "tenant-reporting-closing";
        ingest(tenantId, "CLOSE-1001", "mercado-livre", "RECEIVED", "PIX", "300.00", "255.00", "45.00", "0.00");

        String expenseId = given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(expensePayload("PACKAGING", "Embalagem fechamento", "30.00", true))
                .when().post("/reports/tenants/{tenantId}/expenses", tenantId)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body("""
                        {
                          "signature_hash": "sha256:assinatura-contador-maio"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/closings/2026-05/sign", tenantId)
                .then()
                .statusCode(200)
                .body("tenant_id", is(tenantId))
                .body("period_month", is("2026-05"))
                .body("signed_by_email", is("seller@brasaller.test"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(publicEntryPayload("mercado-livre", "CLOSE-1002", "CANCELLED", "PIX", "300.00", "0.00", "45.00", "0.00"))
                .when().post("/reports/tenants/{tenantId}/manual-import/entries", tenantId)
                .then()
                .statusCode(409)
                .body("message", is("accounting_period_closed"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(expensePayload("PACKAGING", "Edicao bloqueada", "35.00", true))
                .when().put("/reports/tenants/{tenantId}/expenses/{expenseId}", tenantId, expenseId)
                .then()
                .statusCode(409)
                .body("message", is("accounting_period_closed"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().delete("/reports/tenants/{tenantId}/expenses/{expenseId}", tenantId, expenseId)
                .then()
                .statusCode(409)
                .body("message", is("accounting_period_closed"));
    }

    @Test
    void accountantCanReadDreButCannotWriteFiscalData() {
        String tenantId = "tenant-reporting-fiscal-accountant";
        ingest(tenantId, "DRE-ACC-1001", "sandbox", "RECEIVED", "PIX", "100.00", "90.00", "10.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body(fiscalProfilePayload("SIMPLES_NACIONAL", "0.0600"))
                .when().put("/reports/tenants/{tenantId}/fiscal-profile", tenantId)
                .then()
                .statusCode(403);

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body(expensePayload("OTHER", "Despesa contador", "10.00", false))
                .when().post("/reports/tenants/{tenantId}/expenses", tenantId)
                .then()
                .statusCode(403);

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("gross_revenue", is(100.00F))
                .body("marketplace_fees", is(10.00F))
                .body("net_result", is(90.00F));
    }

    @Test
    void accountantCanReadButCannotIngest() {
        String tenantId = "tenant-reporting-accountant";
        ingest(tenantId, "ACC-1001", "sandbox", "RECEIVED", "PIX", "120.00", "110.00", "10.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/summary", tenantId)
                .then()
                .statusCode(200)
                .body("gross_value", is(120.00F));
    }

    private void ingest(String tenantId, String orderId, String platform, String status, String paymentMethod,
                        String grossValue, String receivedValue, String feeValue, String receivableValue) {
        given()
                .contentType("application/json")
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .body(entryPayload(tenantId, orderId, platform, status, paymentMethod, grossValue, receivedValue, feeValue, receivableValue))
                .when().post("/reports/internal/entries")
                .then()
                .statusCode(201);
    }

    private String entryPayload(String tenantId, String orderId, String platform, String status, String paymentMethod) {
        return entryPayload(tenantId, orderId, platform, status, paymentMethod, "199.90", "173.50", "26.40", "0.00");
    }

    private String clicksignPayload(String eventName, String documentKey) {
        return """
                {
                  "event": {
                    "name": "%s",
                    "data": {
                      "account": {
                        "key": "clicksign-account-123"
                      },
                      "envelope": {
                        "id": "clicksign-envelope-123"
                      }
                    },
                    "occurred_at": "2026-06-01T01:15:00.000-03:00"
                  },
                  "document": {
                    "key": "%s"
                  }
                }
                """.formatted(eventName, documentKey);
    }

    private String entryPayload(String tenantId, String orderId, String platform, String status, String paymentMethod,
                                String grossValue, String receivedValue, String feeValue, String receivableValue) {
        return """
                {
                  "tenant_id": "%s",
                  "platform": "%s",
                  "order_id": "%s",
                  "sale_date": "2026-05-21",
                  "gross_value": %s,
                  "received_value": %s,
                  "fee_value": %s,
                  "receivable_value": %s,
                  "payment_method": "%s",
                  "status": "%s",
                  "release_date": "2026-06-04",
                  "buyer_name": "Comprador Teste",
                  "invoice_number": "NF-%s"
                }
                """.formatted(tenantId, platform, orderId, grossValue, receivedValue, feeValue, receivableValue,
                paymentMethod, status, orderId);
    }

    private String fiscalProfilePayload(String taxRegime, String estimatedTaxRate) {
        return """
                {
                  "tax_regime": "%s",
                  "estimated_tax_rate": %s,
                  "notes": "Perfil fiscal usado na DRE MVP"
                }
                """.formatted(taxRegime, estimatedTaxRate);
    }

    private String expensePayload(String category, String description, String amount, boolean withAttachment) {
        String attachment = withAttachment
                ? """
                  "attachment": {
                    "public_id": "brasaller/despesas/embalagem-maio",
                    "secure_url": "https://res.cloudinary.com/brasaller/image/upload/v1/despesas/embalagem-maio.pdf",
                    "resource_type": "image",
                    "original_filename": "embalagem-maio.pdf",
                    "content_type": "application/pdf",
                    "size_bytes": 2048
                  }
                """
                : """
                  "attachment": null
                """;
        return """
                {
                  "expense_date": "2026-05-23",
                  "category": "%s",
                  "description": "%s",
                  "amount": %s,
                  %s
                }
                """.formatted(category, description, amount, attachment);
    }

    private String publicEntryPayload(String platform, String orderId, String status, String paymentMethod,
                                      String grossValue, String receivedValue, String feeValue, String receivableValue) {
        return """
                {
                  "platform": "%s",
                  "order_id": "%s",
                  "sale_date": "2026-05-21",
                  "gross_value": %s,
                  "received_value": %s,
                  "fee_value": %s,
                  "receivable_value": %s,
                  "payment_method": "%s",
                  "status": "%s",
                  "release_date": "2026-06-04",
                  "buyer_name": "Comprador Integrado",
                  "invoice_number": "NF-%s"
                }
                """.formatted(platform, orderId, grossValue, receivedValue, feeValue, receivableValue,
                paymentMethod, status, orderId);
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

    private String contentHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String zipText(byte[] xlsx, String path) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (path.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Missing zip entry: " + path);
    }
}
