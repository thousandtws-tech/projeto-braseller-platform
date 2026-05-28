package com.example.interfaces.rest;

import com.example.application.command.UpsertReportEntryCommand;
import com.example.application.exception.ValidationException;
import com.example.application.service.ReportExportService;
import com.example.application.service.ReportingService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.AvailableFilters;
import com.example.domain.model.DashboardView;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.MonthlyEvolutionPoint;
import com.example.domain.model.PaymentMethod;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportEntryPage;
import com.example.domain.model.ReportEntryStatus;
import com.example.domain.model.ReportExportFile;
import com.example.domain.model.ReportExportFormat;
import com.example.domain.model.ReportFilter;
import com.example.infrastructure.security.ConfiguredInternalServiceAuthorizer;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Path("/reports")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reports", description = "Dashboard, cards financeiros, lancamentos, filtros e graficos.")
public class ReportingResource {
    @Inject
    ReportingService reportingService;

    @Inject
    ReportExportService reportExportService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    ConfiguredInternalServiceAuthorizer internalServiceAuthorizer;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do reporting-service", description = "Verifica se o reporting-service esta respondendo.")
    public String status() {
        return "Reporting Service is running";
    }

    @GET
    @Path("/tenants/{tenantId}/dashboard")
    @Operation(summary = "Painel consolidado", description = "Retorna cards, tabela inicial, graficos e filtros para o painel multi-marketplace.")
    @SecurityRequirement(name = "bearerAuth")
    public DashboardView dashboard(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("platform") String platform,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("status") String status,
            @QueryParam("search") String search,
            @QueryParam("sort") String sort,
            @QueryParam("direction") String direction,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.dashboard(tenantId, filter(from, to, platform, paymentMethod, status, search, sort, direction, page, size));
    }

    @GET
    @Path("/tenants/{tenantId}/summary")
    @Operation(summary = "Cards financeiros", description = "Soma faturado, recebido, taxas e a receber do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public FinancialSummary summary(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("platform") String platform,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("status") String status,
            @QueryParam("search") String search) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.summary(tenantId, filter(from, to, platform, paymentMethod, status, search, null, null, null, null));
    }

    @GET
    @Path("/tenants/{tenantId}/entries")
    @Operation(summary = "Tabela de lancamentos", description = "Lista lancamentos com filtro, ordenacao, busca e paginacao.")
    @SecurityRequirement(name = "bearerAuth")
    public ReportEntryPage entries(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("platform") String platform,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("status") String status,
            @QueryParam("search") String search,
            @QueryParam("sort") String sort,
            @QueryParam("direction") String direction,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.entries(tenantId, filter(from, to, platform, paymentMethod, status, search, sort, direction, page, size));
    }

    @POST
    @Path("/tenants/{tenantId}/manual-import/entries")
    @Operation(summary = "Importar lancamento manual", description = "Cria ou atualiza um lancamento normalizado para plataformas sem API.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PublicReportEntryImportRequest.class)))
    public Response manualImportEntry(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            PublicReportEntryImportRequest request) {
        tenantAuthorizationService.requireIntegrationWritable(authorizationHeader, tenantId);
        return importEntryResponse(tenantId, request);
    }

    @POST
    @Path("/tenants/{tenantId}/integrations/entries")
    @Operation(summary = "API publica de integracao", description = "Permite que vendedor ou contador integre sistemas proprios enviando lancamentos normalizados.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PublicReportEntryImportRequest.class)))
    public Response integrationEntry(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            PublicReportEntryImportRequest request) {
        tenantAuthorizationService.requireIntegrationWritable(authorizationHeader, tenantId);
        return importEntryResponse(tenantId, request);
    }

    @GET
    @Path("/tenants/{tenantId}/charts/monthly-evolution")
    @Operation(summary = "Evolucao mensal", description = "Agrupa faturamento, recebido, taxas e a receber por mes.")
    @SecurityRequirement(name = "bearerAuth")
    public List<MonthlyEvolutionPoint> monthlyEvolution(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("platform") String platform,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("status") String status) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.monthlyEvolution(tenantId, filter(from, to, platform, paymentMethod, status, null, null, null, null, null));
    }

    @GET
    @Path("/tenants/{tenantId}/charts/platform-comparison")
    @Operation(summary = "Comparativo por plataforma", description = "Agrupa valores financeiros por marketplace/plataforma.")
    @SecurityRequirement(name = "bearerAuth")
    public List<PlatformComparisonPoint> platformComparison(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("paymentMethod") String paymentMethod,
            @QueryParam("status") String status) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.platformComparison(tenantId, filter(from, to, null, paymentMethod, status, null, null, null, null, null));
    }

    @GET
    @Path("/tenants/{tenantId}/filters")
    @Operation(summary = "Filtros disponiveis", description = "Lista plataformas, formas de pagamento e status existentes no read model.")
    @SecurityRequirement(name = "bearerAuth")
    public AvailableFilters filters(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return reportingService.filters(tenantId);
    }

    @GET
    @Path("/tenants/{tenantId}/exports/monthly")
    @Produces({"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv"})
    @Operation(summary = "Exportar relatorio mensal consolidado", description = "Gera PDF, XLSX ou CSV com todos os marketplaces do mes informado.")
    @SecurityRequirement(name = "bearerAuth")
    public Response exportMonthly(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("month") String month,
            @QueryParam("format") String format) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return exportResponse(reportExportService.exportMonthly(
                tenantId,
                parseMonth(month),
                parseExportFormat(format)
        ));
    }

    @GET
    @Path("/tenants/{tenantId}/exports/platforms/{platform}")
    @Produces({"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv"})
    @Operation(summary = "Exportar relatorio por plataforma", description = "Gera PDF, XLSX ou CSV filtrado por marketplace/modulo.")
    @SecurityRequirement(name = "bearerAuth")
    public Response exportPlatform(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("platform") String platform,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("format") String format) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return exportResponse(reportExportService.exportPlatform(
                tenantId,
                platform,
                from,
                to,
                parseExportFormat(format)
        ));
    }

    @POST
    @Path("/internal/entries")
    @Operation(summary = "Ingerir lancamento", description = "Endpoint interno para materializar lancamentos vindos do Core/conectores.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ReportEntryIngestRequest.class)))
    public Response ingest(@HeaderParam("X-Internal-Token") String internalToken, ReportEntryIngestRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        ReportEntry entry = reportingService.upsert(new UpsertReportEntryCommand(
                request.tenantId(),
                request.platform(),
                request.orderId(),
                request.saleDate(),
                request.grossValue(),
                request.receivedValue(),
                request.feeValue(),
                request.receivableValue(),
                parsePaymentMethod(request.paymentMethod()),
                parseStatus(request.status()),
                request.releaseDate(),
                request.buyerName(),
                request.invoiceNumber()
        ));
        return Response.status(Response.Status.CREATED).entity(entry).build();
    }

    @GET
    @Path("/internal/tenants/{tenantId}/summary")
    @Operation(summary = "Resumo interno para automacoes", description = "Retorna resumo financeiro para chamadas service-to-service.")
    @SecurityRequirement(name = "internalToken")
    public FinancialSummary internalSummary(
            @HeaderParam("X-Internal-Token") String internalToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return reportingService.summary(tenantId, filter(from, to, null, null, null, null, null, null, null, null));
    }

    @GET
    @Path("/internal/tenants/{tenantId}/payment-releases")
    @Operation(summary = "Pagamentos a liberar", description = "Lista pagamentos a liberar por plataforma para alertas automaticos.")
    @SecurityRequirement(name = "internalToken")
    public List<PaymentReleaseAlert> internalPaymentReleases(
            @HeaderParam("X-Internal-Token") String internalToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("platform") String platform,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return reportingService.paymentReleases(tenantId, platform, from, to);
    }

    private ReportFilter filter(
            LocalDate from,
            LocalDate to,
            String platform,
            String paymentMethod,
            String status,
            String search,
            String sort,
            String direction,
            Integer page,
            Integer size) {
        return new ReportFilter(
                from,
                to,
                platform,
                parsePaymentMethod(paymentMethod),
                parseStatus(status),
                search,
                sort,
                direction,
                page,
                size
        );
    }

    private Response exportResponse(ReportExportFile file) {
        return Response.ok(file.content(), file.mediaType())
                .header("Content-Disposition", "attachment; filename=\"" + file.fileName() + "\"")
                .header("X-Report-Filename", file.fileName())
                .build();
    }

    private Response importEntryResponse(String tenantId, PublicReportEntryImportRequest request) {
        if (request == null) {
            throw new ValidationException("report entry is required");
        }
        ReportEntry entry = reportingService.upsert(new UpsertReportEntryCommand(
                tenantId,
                request.platform(),
                request.orderId(),
                request.saleDate(),
                request.grossValue(),
                request.receivedValue(),
                request.feeValue(),
                request.receivableValue(),
                parsePaymentMethod(request.paymentMethod()),
                parseStatus(request.status()),
                request.releaseDate(),
                request.buyerName(),
                request.invoiceNumber()
        ));
        return Response.status(Response.Status.CREATED).entity(entry).build();
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ValidationException("invalid_month");
        }
    }

    private ReportExportFormat parseExportFormat(String value) {
        if (value == null || value.isBlank()) {
            return ReportExportFormat.PDF;
        }
        String normalized = value.trim().replace(".", "").replace("-", "_").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PDF" -> ReportExportFormat.PDF;
            case "XLSX", "XLS", "EXCEL" -> ReportExportFormat.XLSX;
            case "CSV" -> ReportExportFormat.CSV;
            default -> throw new ValidationException("invalid_export_format");
        };
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("invalid_payment_method");
        }
    }

    private ReportEntryStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return ReportEntryStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("invalid_status");
        }
    }

    @Schema(name = "ReportEntryIngestRequest")
    public record ReportEntryIngestRequest(
            @JsonProperty("tenant_id") String tenantId,
            @JsonProperty("platform") String platform,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("sale_date") LocalDate saleDate,
            @JsonProperty("gross_value") BigDecimal grossValue,
            @JsonProperty("received_value") BigDecimal receivedValue,
            @JsonProperty("fee_value") BigDecimal feeValue,
            @JsonProperty("receivable_value") BigDecimal receivableValue,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("status") String status,
            @JsonProperty("release_date") LocalDate releaseDate,
            @JsonProperty("buyer_name") String buyerName,
            @JsonProperty("invoice_number") String invoiceNumber) {
    }

    @Schema(name = "PublicReportEntryImportRequest")
    public record PublicReportEntryImportRequest(
            @JsonProperty("platform") String platform,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("sale_date") LocalDate saleDate,
            @JsonProperty("gross_value") BigDecimal grossValue,
            @JsonProperty("received_value") BigDecimal receivedValue,
            @JsonProperty("fee_value") BigDecimal feeValue,
            @JsonProperty("receivable_value") BigDecimal receivableValue,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("status") String status,
            @JsonProperty("release_date") LocalDate releaseDate,
            @JsonProperty("buyer_name") String buyerName,
            @JsonProperty("invoice_number") String invoiceNumber) {
    }
}
