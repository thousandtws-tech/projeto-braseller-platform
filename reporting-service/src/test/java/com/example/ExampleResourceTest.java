package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ExampleResourceTest {
    @Inject
    DataSource dataSource;

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
                .when().get("/reports/tenants/{tenantId}/expenses?from=2026-05-01&to=2026-05-31&page=0&size=20", tenantId)
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items.category", hasItems("PACKAGING", "BANK_FEE"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("tax_regime", is("LUCRO_PRESUMIDO"))
                .body("gross_revenue", is(500.00F))
                .body("marketplace_fees", is(65.00F))
                .body("estimated_tax_rate", is(0.0593F))
                .body("estimated_taxes", is(29.65F))
                .body("operating_expenses", is(60.00F))
                .body("net_result", is(345.35F))
                .body("expense_count", is(2))
                .body("expenses_by_category.category", hasItems("PACKAGING", "BANK_FEE"));
    }

    @Test
    void dreCalculatesSimplesNacionalEffectiveRateByRevenueBracket() {
        String tenantId = "tenant-reporting-tax-simples";
        ingest(tenantId, "TAX-SIMPLES-1001", "mercado-livre", "RECEIVED", "PIX", "200000.00", "200000.00", "0.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(fiscalProfilePayload("SIMPLES_NACIONAL", "0.0000"))
                .when().put("/reports/tenants/{tenantId}/fiscal-profile", tenantId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("tax_regime", is("SIMPLES_NACIONAL"))
                .body("gross_revenue", is(200000.00F))
                .body("estimated_tax_rate", is(0.0433F))
                .body("estimated_taxes", is(8660.00F))
                .body("net_result", is(191340.00F));
    }

    @Test
    void dreCalculatesLucroRealOverAccountingProfitAndRevenueTaxes() {
        String tenantId = "tenant-reporting-tax-real";
        ingest(tenantId, "TAX-REAL-1001", "mercado-livre", "RECEIVED", "PIX", "100000.00", "90000.00", "10000.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(fiscalProfilePayload("LUCRO_REAL", "0.0000"))
                .when().put("/reports/tenants/{tenantId}/fiscal-profile", tenantId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("tax_regime", is("LUCRO_REAL"))
                .body("gross_revenue", is(100000.00F))
                .body("marketplace_fees", is(10000.00F))
                .body("estimated_tax_rate", is(0.3785F))
                .body("estimated_taxes", is(37850.00F))
                .body("net_result", is(52150.00F));
    }

    @Test
    void stockItemsUseSnakeCaseMoneyFieldsAndNfeImportUpdatesQuantity() {
        String tenantId = "tenant-reporting-stock";

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body(stockItemPayload("MANUAL-001", "Produto manual", "12.34"))
                .when().post("/reports/tenants/{tenantId}/stock/items", tenantId)
                .then()
                .statusCode(200)
                .body("tenant_id", is(tenantId))
                .body("sku", is("MANUAL-001"))
                .body("unit_cost", is(12.34F))
                .body("quantity", is(0.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("text/xml")
                .body(nfeXml("NFE-001", "Produto NF-e", "3.0000", "7.50", "22.50"))
                .when().post("/reports/tenants/{tenantId}/stock/nfe-import", tenantId)
                .then()
                .statusCode(200)
                .body("total_cost", is(22.50F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/stock/items", tenantId)
                .then()
                .statusCode(200)
                .body("find { it.sku == 'MANUAL-001' }.unit_cost", is(12.34F))
                .body("find { it.sku == 'MANUAL-001' }.quantity", is(0.00F))
                .body("find { it.sku == 'NFE-001' }.unit_cost", is(7.50F))
                .body("find { it.sku == 'NFE-001' }.quantity", is(3.0000F));
    }

    @Test
    void salesWithItemsCreateIdempotentStockExitAndFeedCmv() {
        String tenantId = "tenant-reporting-cmv-stock-exit";

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("text/xml")
                .body(nfeXml("SKU-CMV", "Produto com CMV", "5.0000", "10.00", "50.00"))
                .when().post("/reports/tenants/{tenantId}/stock/nfe-import", tenantId)
                .then()
                .statusCode(200);

        ingestWithItem(tenantId, "CMV-1001", "mercado-livre", "RECEIVED", "PIX",
                "100.00", "85.00", "15.00", "0.00", "SKU-CMV", "2.0000");
        ingestWithItem(tenantId, "CMV-1001", "mercado-livre", "RECEIVED", "PIX",
                "100.00", "85.00", "15.00", "0.00", "SKU-CMV", "2.0000");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/stock/items", tenantId)
                .then()
                .statusCode(200)
                .body("find { it.sku == 'SKU-CMV' }.quantity", is(3.0000F))
                .body("find { it.sku == 'SKU-CMV' }.unit_cost", is(10.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("gross_revenue", is(100.00F))
                .body("marketplace_fees", is(15.00F))
                .body("cmv", is(20.00F))
                .body("net_result", is(65.00F))
                .body("distributable_profit", is(65.00F));
    }

    @Test
    void cancelledSaleReversesStockExitAndCmvIdempotently() {
        String tenantId = "tenant-reporting-cmv-stock-reversal";

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("text/xml")
                .body(nfeXml("SKU-REV", "Produto cancelado", "5.0000", "10.00", "50.00"))
                .when().post("/reports/tenants/{tenantId}/stock/nfe-import", tenantId)
                .then()
                .statusCode(200);

        ingestWithItem(tenantId, "CMV-CANCEL-1001", "mercado-livre", "RECEIVED", "PIX",
                "100.00", "85.00", "15.00", "0.00", "SKU-REV", "2.0000");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("cmv", is(20.00F));

        ingestWithItem(tenantId, "CMV-CANCEL-1001", "mercado-livre", "CANCELLED", "PIX",
                "100.00", "85.00", "15.00", "0.00", "SKU-REV", "2.0000");
        ingestWithItem(tenantId, "CMV-CANCEL-1001", "mercado-livre", "CANCELLED", "PIX",
                "100.00", "85.00", "15.00", "0.00", "SKU-REV", "2.0000");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/entries?search=CMV-CANCEL-1001", tenantId)
                .then()
                .statusCode(200)
                .body("items[0].status", is("CANCELLED"))
                .body("items[0].gross_value", is(0.00F))
                .body("items[0].received_value", is(0.00F))
                .body("items[0].fee_value", is(0.00F))
                .body("items[0].receivable_value", is(0.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/stock/items", tenantId)
                .then()
                .statusCode(200)
                .body("find { it.sku == 'SKU-REV' }.quantity", is(5.0000F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/dre?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("gross_revenue", is(0.00F))
                .body("cmv", is(0.00F))
                .body("net_result", is(0.00F))
                .body("distributable_profit", is(0.00F));
    }

    @Test
    void cancelledAndRefundedLegacyValuesAreIgnoredByFinancialAggregates() {
        String tenantId = "tenant-reporting-cancelled-revenue-summary";
        ingest(tenantId, "ACTIVE-1001", "mercado-livre", "RECEIVED", "PIX", "100.00", "85.00", "15.00", "0.00");
        insertReportEntry(tenantId, "LEGACY-CANCEL-1002", "shopee", "CANCELLED", "300.00", "0.00", "45.00", "0.00");
        insertReportEntry(tenantId, "LEGACY-REFUND-1003", "amazon", "REFUNDED", "80.00", "70.00", "10.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/summary?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("gross_value", is(100.00F))
                .body("received_value", is(85.00F))
                .body("fee_value", is(15.00F))
                .body("receivable_value", is(0.00F))
                .body("entry_count", is(1));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/entries?search=LEGACY-CANCEL-1002", tenantId)
                .then()
                .statusCode(200)
                .body("items[0].status", is("CANCELLED"))
                .body("items[0].gross_value", is(0.00F))
                .body("items[0].received_value", is(0.00F))
                .body("items[0].fee_value", is(0.00F))
                .body("items[0].receivable_value", is(0.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/charts/platform-comparison?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].platform", is("mercado-livre"))
                .body("[0].gross_value", is(100.00F))
                .body("[0].entry_count", is(1));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/charts/monthly-evolution?from=2026-05-01&to=2026-05-31", tenantId)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].gross_value", is(100.00F))
                .body("[0].entry_count", is(1));
    }

    @Test
    void expensesListToleratesLegacyCategoryValues() {
        String tenantId = "tenant-reporting-legacy-expense";
        insertExpenseWithCategory(tenantId, "Embalagem");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/expenses?from=2026-06-01&to=2026-06-30&page=0&size=20", tenantId)
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("items.category", hasItems("OTHER"));
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
    void signedClosingReleasesProfitAndDistributionConsumesBalance() {
        String tenantId = "tenant-reporting-profit-available";
        ingest(tenantId, "PROFIT-1001", "mercado-livre", "RECEIVED", "PIX", "300.00", "255.00", "45.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body("""
                        {
                          "signature_hash": "sha256:lucro-disponivel-maio"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/closings/2026-05/sign", tenantId)
                .then()
                .statusCode(200)
                .body("distributable_profit", is(255.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/profit/available", tenantId)
                .then()
                .statusCode(200)
                .body("total_released_profit", is(255.00F))
                .body("total_distributed_profit", is(0))
                .body("available_profit", is(255.00F))
                .body("periods[0].period_month", is("2026-05"))
                .body("periods[0].available_profit", is(255.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body("""
                        {
                          "period_month": "2026-05",
                          "amount": 100.00,
                          "distributed_at": "2026-06-08",
                          "recipient_name": "Socio administrador",
                          "notes": "Retirada mensal"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/profit/distributions", tenantId)
                .then()
                .statusCode(201)
                .body("tenant_id", is(tenantId))
                .body("period_month", is("2026-05"))
                .body("amount", is(100.00F))
                .body("created_by_email", is("seller@brasaller.test"));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .when().get("/reports/tenants/{tenantId}/profit/available", tenantId)
                .then()
                .statusCode(200)
                .body("total_released_profit", is(255.00F))
                .body("total_distributed_profit", is(100.00F))
                .body("available_profit", is(155.00F))
                .body("periods[0].distributed_profit", is(100.00F))
                .body("periods[0].available_profit", is(155.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .when().get("/reports/tenants/{tenantId}/profit/distributions?month=2026-05", tenantId)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].recipient_name", is("Socio administrador"));
    }

    @Test
    void profitDistributionCannotExceedSignedClosingBalance() {
        String tenantId = "tenant-reporting-profit-limit";
        ingest(tenantId, "PROFIT-LIMIT-1001", "mercado-livre", "RECEIVED", "PIX", "120.00", "110.00", "10.00", "0.00");

        given()
                .header("Authorization", "Bearer " + token(tenantId, "CONTADOR"))
                .contentType("application/json")
                .body("""
                        {
                          "signature_hash": "sha256:lucro-limite-maio"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/closings/2026-05/sign", tenantId)
                .then()
                .statusCode(200)
                .body("distributable_profit", is(110.00F));

        given()
                .header("Authorization", "Bearer " + token(tenantId, "ADMIN"))
                .contentType("application/json")
                .body("""
                        {
                          "period_month": "2026-05",
                          "amount": 111.00,
                          "distributed_at": "2026-06-08"
                        }
                        """)
                .when().post("/reports/tenants/{tenantId}/profit/distributions", tenantId)
                .then()
                .statusCode(400)
                .body("message", is("insufficient_distributable_profit"));
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

    @Test
    void accountantCanReadGrantedBpoTenantButCannotWriteIt() {
        String primaryTenantId = "tenant-reporting-bpo-primary";
        String clientTenantId = "tenant-reporting-bpo-client";
        ingest(clientTenantId, "BPO-1001", "mercado-livre", "RECEIVED", "PIX", "240.00", "210.00", "30.00", "0.00");

        String accountantToken = token(primaryTenantId, List.of(clientTenantId), "CONTADOR");

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .when().get("/reports/tenants/{tenantId}/summary", clientTenantId)
                .then()
                .statusCode(200)
                .body("gross_value", is(240.00F));

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body(fiscalProfilePayload("SIMPLES_NACIONAL", "0.0600"))
                .when().put("/reports/tenants/{tenantId}/fiscal-profile", clientTenantId)
                .then()
                .statusCode(403)
                .body("message", is("tenant_mismatch"));
    }

    @Test
    void accountantCanBatchSignGrantedBpoClientClosings() {
        String primaryTenantId = "tenant-reporting-bpo-batch-primary";
        String firstClientTenantId = "tenant-reporting-bpo-batch-client-a";
        String secondClientTenantId = "tenant-reporting-bpo-batch-client-b";
        ingest(firstClientTenantId, "BPO-BATCH-1001", "mercado-livre", "RECEIVED", "PIX", "300.00", "270.00", "30.00", "0.00");
        ingest(secondClientTenantId, "BPO-BATCH-2001", "shopee", "RECEIVED", "PIX", "500.00", "430.00", "70.00", "0.00");

        String accountantToken = token(primaryTenantId, List.of(firstClientTenantId, secondClientTenantId), "CONTADOR");

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body("""
                        {
                          "tenant_ids": ["%s", "%s"],
                          "signature_hash": "sha256:bpo-lote-junho"
                        }
                        """.formatted(firstClientTenantId, secondClientTenantId))
                .when().post("/reports/bpo/closings/2026-06/batch-sign")
                .then()
                .statusCode(200)
                .body("period_month", is("2026-06"))
                .body("requested_count", is(2))
                .body("signed_count", is(2))
                .body("skipped_count", is(0))
                .body("failed_count", is(0))
                .body("results.status", contains("SIGNED", "SIGNED"))
                .body("results.closing.tenant_id", contains(firstClientTenantId, secondClientTenantId));

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body("""
                        {
                          "tenant_ids": ["%s", "%s"],
                          "signature_hash": "sha256:bpo-lote-junho-retry"
                        }
                        """.formatted(firstClientTenantId, secondClientTenantId))
                .when().post("/reports/bpo/closings/2026-06/batch-sign")
                .then()
                .statusCode(200)
                .body("signed_count", is(0))
                .body("skipped_count", is(2))
                .body("results.status", contains("SKIPPED", "SKIPPED"));
    }

    @Test
    void batchBpoClosingRejectsTenantOutsideAccountantPortfolio() {
        String primaryTenantId = "tenant-reporting-bpo-batch-forbidden-primary";
        String grantedTenantId = "tenant-reporting-bpo-batch-granted";
        String forbiddenTenantId = "tenant-reporting-bpo-batch-forbidden";
        String accountantToken = token(primaryTenantId, List.of(grantedTenantId), "CONTADOR");

        given()
                .header("Authorization", "Bearer " + accountantToken)
                .contentType("application/json")
                .body("""
                        {
                          "tenant_ids": ["%s", "%s"],
                          "signature_hash": "sha256:bpo-forbidden"
                        }
                        """.formatted(grantedTenantId, forbiddenTenantId))
                .when().post("/reports/bpo/closings/2026-06/batch-sign")
                .then()
                .statusCode(403)
                .body("message", is("tenant_mismatch"));
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

    private void ingestWithItem(String tenantId, String orderId, String platform, String status, String paymentMethod,
                                String grossValue, String receivedValue, String feeValue, String receivableValue,
                                String sku, String quantity) {
        given()
                .contentType("application/json")
                .header("X-Internal-Token", "dev-internal-token-change-me")
                .body(entryPayloadWithItem(tenantId, orderId, platform, status, paymentMethod,
                        grossValue, receivedValue, feeValue, receivableValue, sku, quantity))
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

    private String entryPayloadWithItem(String tenantId, String orderId, String platform, String status, String paymentMethod,
                                        String grossValue, String receivedValue, String feeValue, String receivableValue,
                                        String sku, String quantity) {
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
                  "items": [
                    {
                      "sku": "%s",
                      "title": "Produto com CMV",
                      "quantity": %s,
                      "unit_value": 50.00,
                      "gross_value": %s
                    }
                  ],
                  "invoice_number": "NF-%s"
                }
                """.formatted(tenantId, platform, orderId, grossValue, receivedValue, feeValue, receivableValue,
                paymentMethod, status, sku, quantity, grossValue, orderId);
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

    private void insertExpenseWithCategory(String tenantId, String category) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO expense_entries
                     (id, tenant_id, expense_date, category, description, amount,
                      attachment_public_id, attachment_secure_url, attachment_resource_type,
                      attachment_original_filename, attachment_content_type, attachment_size_bytes,
                      created_at, updated_at)
                     VALUES (?, ?, DATE '2026-06-04', ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {
            statement.setString(1, "00000000-0000-4000-8000-000000000001");
            statement.setString(2, tenantId);
            statement.setString(3, category);
            statement.setString(4, "Despesa legada");
            statement.setBigDecimal(5, new BigDecimal("25.00"));
            statement.setString(6, "brasaller/despesas/legacy");
            statement.setString(7, "https://res.cloudinary.com/brasaller/image/upload/v1/despesas/legacy.pdf");
            statement.setString(8, "image");
            statement.setString(9, "legacy.pdf");
            statement.setString(10, "application/pdf");
            statement.setLong(11, 2048L);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert legacy expense", exception);
        }
    }

    private void insertReportEntry(String tenantId, String orderId, String platform, String status,
                                   String grossValue, String receivedValue, String feeValue, String receivableValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO report_entries
                     (id, tenant_id, platform, order_id, sale_date, gross_value, received_value,
                      fee_value, receivable_value, payment_method, status, release_date,
                      buyer_name, invoice_number, created_at, updated_at)
                     VALUES (?, ?, ?, ?, DATE '2026-05-21', ?, ?, ?, ?, 'PIX', ?, DATE '2026-06-04',
                             'Comprador Legado', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {
            statement.setString(1, "legacy-" + orderId);
            statement.setString(2, tenantId);
            statement.setString(3, platform);
            statement.setString(4, orderId);
            statement.setBigDecimal(5, new BigDecimal(grossValue));
            statement.setBigDecimal(6, new BigDecimal(receivedValue));
            statement.setBigDecimal(7, new BigDecimal(feeValue));
            statement.setBigDecimal(8, new BigDecimal(receivableValue));
            statement.setString(9, status);
            statement.setString(10, "NF-" + orderId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert legacy report entry", exception);
        }
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

    private String stockItemPayload(String sku, String description, String unitCost) {
        return """
                {
                  "sku": "%s",
                  "description": "%s",
                  "unit_cost": "%s"
                }
                """.formatted(sku, description, unitCost);
    }

    private String nfeXml(String sku, String description, String quantity, String unitCost, String totalCost) {
        return """
                <NFe>
                  <infNFe>
                    <ide>
                      <serie>1</serie>
                      <nNF>12345</nNF>
                      <dhEmi>2026-05-20T10:00:00-03:00</dhEmi>
                    </ide>
                    <emit>
                      <xNome>Fornecedor Teste</xNome>
                    </emit>
                    <det nItem="1">
                      <prod>
                        <cProd>%s</cProd>
                        <xProd>%s</xProd>
                        <qCom>%s</qCom>
                        <vUnCom>%s</vUnCom>
                        <vProd>%s</vProd>
                      </prod>
                    </det>
                    <total>
                      <ICMSTot>
                        <vNF>%s</vNF>
                      </ICMSTot>
                    </total>
                  </infNFe>
                </NFe>
                """.formatted(sku, description, quantity, unitCost, totalCost, totalCost);
    }

    private String token(String tenantId, String... roles) {
        return token(tenantId, List.of(), roles);
    }

    private String token(String tenantId, List<String> accountantTenantIds, String... roles) {
        String header = encode("""
                {"alg":"HS256","typ":"JWT"}
                """);
        long expiration = Instant.now().plusSeconds(300).getEpochSecond();
        String groups = Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", "));
        String accountantTenants = accountantTenantIds.stream()
                .map(tenant -> "\"" + tenant + "\"")
                .collect(Collectors.joining(", "));
        String payload = encode("""
                {
                  "iss": "brasaller-auth",
                  "aud": "brasaller-platform",
                  "exp": %d,
                  "tenant_id": "%s",
                  "user_id": "user-123",
                  "email": "seller@brasaller.test",
                  "groups": [%s],
                  "accountant_tenant_ids": [%s]
                }
                """.formatted(expiration, tenantId, groups, accountantTenants));
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
