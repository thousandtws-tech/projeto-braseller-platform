package com.example.interfaces.rest;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertReportEntryCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.application.exception.ValidationException;
import com.example.application.service.ClicksignWebhookService;
import com.example.application.service.CloudinaryUploadSignatureService;
import com.example.application.service.FiscalAccountingService;
import com.example.application.service.ReportExportService;
import com.example.application.service.ReportingService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.AccountingPeriodClosing;
import com.example.domain.model.AvailableFilters;
import com.example.domain.model.ClicksignWebhookEvent;
import com.example.domain.model.CloudinaryUploadSignature;
import com.example.domain.model.DashboardView;
import com.example.domain.model.DreCalculationJob;
import com.example.domain.model.DreStatement;
import com.example.domain.model.ExpenseAttachment;
import com.example.domain.model.ExpenseCategory;
import com.example.domain.model.ExpenseEntry;
import com.example.domain.model.ExpenseFilter;
import com.example.domain.model.ExpensePage;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.FiscalProfile;
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
import com.example.domain.model.TaxRegime;
import com.example.domain.model.TenantContext;
import com.example.infrastructure.security.ConfiguredInternalServiceAuthorizer;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PUT;
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
    FiscalAccountingService fiscalAccountingService;

    @Inject
    CloudinaryUploadSignatureService cloudinaryUploadSignatureService;

    @Inject
    ClicksignWebhookService clicksignWebhookService;

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

    @POST
    @Path("/webhooks/clicksign")
    @Operation(summary = "Webhook Clicksign", description = "Recebe eventos Clicksign assinados via Content-Hmac para auditoria e conciliacao do fechamento contabil.")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public ClicksignWebhookEvent clicksignWebhook(
            @HeaderParam("Content-Hmac") String contentHmac,
            String payload) {
        return clicksignWebhookService.receive(payload, contentHmac);
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
    @Path("/tenants/{tenantId}/fiscal-profile")
    @Operation(summary = "Consultar perfil fiscal", description = "Retorna o regime tributario e a aliquota estimada usados na DRE.")
    @SecurityRequirement(name = "bearerAuth")
    public FiscalProfile fiscalProfile(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.profile(tenantId);
    }

    @PUT
    @Path("/tenants/{tenantId}/fiscal-profile")
    @Operation(summary = "Salvar perfil fiscal", description = "Cadastra ou atualiza regime tributario e aliquota estimada do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = FiscalProfileRequest.class)))
    public FiscalProfile upsertFiscalProfile(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            FiscalProfileRequest request) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("fiscal profile is required");
        }
        return fiscalAccountingService.upsertProfile(new UpsertFiscalProfileCommand(
                tenantId,
                parseTaxRegime(request.taxRegime()),
                request.estimatedTaxRate(),
                request.notes()
        ));
    }

    @GET
    @Path("/tenants/{tenantId}/expenses/upload-signature")
    @Operation(summary = "Assinar upload Cloudinary", description = "Gera parametros assinados para upload direto de comprovantes de despesa no Cloudinary.")
    @SecurityRequirement(name = "bearerAuth")
    public CloudinaryUploadSignature expenseUploadSignature(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        return cloudinaryUploadSignatureService.expenseUploadSignature(tenantId);
    }

    @GET
    @Path("/tenants/{tenantId}/expenses")
    @Operation(summary = "Listar despesas", description = "Lista despesas operacionais com metadados de anexos Cloudinary.")
    @SecurityRequirement(name = "bearerAuth")
    public ExpensePage expenses(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("category") String category,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.expenses(tenantId, new ExpenseFilter(from, to, parseExpenseCategory(category), page, size));
    }

    @POST
    @Path("/tenants/{tenantId}/expenses")
    @Operation(summary = "Criar despesa", description = "Lanca uma despesa manual com comprovante obrigatorio armazenado no Cloudinary.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ExpenseRequest.class)))
    public Response createExpense(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            ExpenseRequest request) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("expense is required");
        }
        ExpenseEntry expense = fiscalAccountingService.createExpense(new CreateExpenseCommand(
                tenantId,
                request.expenseDate(),
                parseExpenseCategory(request.category()),
                request.description(),
                request.amount(),
                attachment(request.attachment())
        ));
        return Response.status(Response.Status.CREATED).entity(expense).build();
    }

    @GET
    @Path("/tenants/{tenantId}/expenses/{expenseId}")
    @Operation(summary = "Detalhar despesa", description = "Retorna uma despesa especifica do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public ExpenseEntry expense(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("expenseId") String expenseId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.expense(tenantId, expenseId);
    }

    @PUT
    @Path("/tenants/{tenantId}/expenses/{expenseId}")
    @Operation(summary = "Atualizar despesa", description = "Atualiza uma despesa manual e seu anexo Cloudinary.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ExpenseRequest.class)))
    public ExpenseEntry updateExpense(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("expenseId") String expenseId,
            ExpenseRequest request) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("expense is required");
        }
        return fiscalAccountingService.updateExpense(new UpdateExpenseCommand(
                tenantId,
                expenseId,
                request.expenseDate(),
                parseExpenseCategory(request.category()),
                request.description(),
                request.amount(),
                attachment(request.attachment())
        ));
    }

    @DELETE
    @Path("/tenants/{tenantId}/expenses/{expenseId}")
    @Operation(summary = "Remover despesa", description = "Remove uma despesa manual do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public Response deleteExpense(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("expenseId") String expenseId) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        fiscalAccountingService.deleteExpense(tenantId, expenseId);
        return Response.noContent().build();
    }

    @GET
    @Path("/tenants/{tenantId}/dre")
    @Operation(summary = "Gerar DRE", description = "Gera uma DRE simplificada a partir de vendas, taxas, regime tributario e despesas.")
    @SecurityRequirement(name = "bearerAuth")
    public DreStatement dre(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.dre(tenantId, from, to);
    }

    @POST
    @Path("/tenants/{tenantId}/dre/jobs")
    @Operation(summary = "Enfileirar calculo de DRE", description = "Cria um job em banco para calcular a DRE do periodo em background e materializar o resultado.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DreCalculationRequest.class)))
    public Response requestDreCalculation(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            DreCalculationRequest request) {
        TenantContext requester = tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        DreCalculationJob job = fiscalAccountingService.requestDreCalculation(
                tenantId,
                request == null ? null : request.from(),
                request == null ? null : request.to(),
                requester
        );
        return Response.accepted(job).build();
    }

    @GET
    @Path("/tenants/{tenantId}/dre/jobs/{jobId}")
    @Operation(summary = "Consultar job de DRE", description = "Retorna status e resultado materializado do calculo assincrono de DRE.")
    @SecurityRequirement(name = "bearerAuth")
    public DreCalculationJob dreCalculationJob(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("jobId") String jobId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.dreCalculationJob(tenantId, jobId);
    }

    @GET
    @Path("/tenants/{tenantId}/closings/{month}")
    @Operation(summary = "Consultar fechamento contabil", description = "Retorna a assinatura digital que travou o periodo contabil.")
    @SecurityRequirement(name = "bearerAuth")
    public AccountingPeriodClosing accountingClosing(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("month") String month) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.closing(tenantId, parseMonth(month));
    }

    @POST
    @Path("/tenants/{tenantId}/closings/{month}/sign")
    @Operation(summary = "Assinar fechamento contabil", description = "Assinatura do contador que torna imutaveis pedidos, taxas e despesas do mes.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AccountingPeriodSignatureRequest.class)))
    public AccountingPeriodClosing signAccountingClosing(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("month") String month,
            AccountingPeriodSignatureRequest request) {
        TenantContext signer = tenantAuthorizationService.requireClosingSigner(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("closing signature is required");
        }
        return fiscalAccountingService.signClosing(tenantId, parseMonth(month), signer, request.signatureHash());
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

    private TaxRegime parseTaxRegime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return TaxRegime.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("invalid_tax_regime");
        }
    }

    private ExpenseCategory parseExpenseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return ExpenseCategory.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("invalid_expense_category");
        }
    }

    private ExpenseAttachment attachment(CloudinaryAttachmentRequest request) {
        if (request == null) {
            return null;
        }
        return new ExpenseAttachment(
                request.publicId(),
                request.secureUrl(),
                request.resourceType(),
                request.originalFilename(),
                request.contentType(),
                request.sizeBytes()
        );
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

    @Schema(name = "FiscalProfileRequest")
    public record FiscalProfileRequest(
            @JsonProperty("tax_regime") String taxRegime,
            @JsonProperty("estimated_tax_rate") BigDecimal estimatedTaxRate,
            @JsonProperty("notes") String notes) {
    }

    @Schema(name = "ExpenseRequest")
    public record ExpenseRequest(
            @JsonProperty("expense_date") LocalDate expenseDate,
            @JsonProperty("category") String category,
            @JsonProperty("description") String description,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("attachment") CloudinaryAttachmentRequest attachment) {
    }

    @Schema(name = "CloudinaryAttachmentRequest")
    public record CloudinaryAttachmentRequest(
            @JsonProperty("public_id") String publicId,
            @JsonProperty("secure_url") String secureUrl,
            @JsonProperty("resource_type") String resourceType,
            @JsonProperty("original_filename") String originalFilename,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("size_bytes") Long sizeBytes) {
    }

    @Schema(name = "AccountingPeriodSignatureRequest")
    public record AccountingPeriodSignatureRequest(
            @JsonProperty("signature_hash") String signatureHash) {
    }

    @Schema(name = "DreCalculationRequest")
    public record DreCalculationRequest(
            @JsonProperty("from") LocalDate from,
            @JsonProperty("to") LocalDate to) {
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
