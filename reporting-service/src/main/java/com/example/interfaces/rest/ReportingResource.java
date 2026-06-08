package com.example.interfaces.rest;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.CreateProfitDistributionCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertReportEntryCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.application.exception.NotFoundException;
import com.example.application.exception.ValidationException;
import com.example.application.service.ClicksignWebhookService;
import com.example.application.service.CloudinaryUploadSignatureService;
import com.example.application.service.FiscalAccountingService;
import com.example.application.service.BankTransactionService;
import com.example.application.service.InvoiceTrackingService;
import com.example.application.service.StockService;
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
import com.example.domain.model.BankTransaction;
import com.example.domain.model.InvoiceEntry;
import com.example.domain.model.PurchaseEntry;
import com.example.domain.model.StockItem;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.FiscalProfile;
import com.example.domain.model.MonthlyEvolutionPoint;
import com.example.domain.model.PaymentMethod;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ProfitAvailability;
import com.example.domain.model.ProfitDistribution;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    @Inject
    InvoiceTrackingService invoiceTrackingService;

    @Inject
    StockService stockService;

    @Inject
    BankTransactionService bankTransactionService;

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

    @POST
    @Path("/bpo/closings/{month}/batch-sign")
    @Operation(summary = "Assinar fechamentos em lote", description = "Permite ao contador assinar fechamentos de varios clientes vinculados em uma unica operacao BPO.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BatchAccountingPeriodSignatureRequest.class)))
    public BatchAccountingPeriodSignatureResponse batchSignAccountingClosings(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("month") String month,
            BatchAccountingPeriodSignatureRequest request) {
        YearMonth periodMonth = parseMonth(month);
        List<String> tenantIds = normalizeTenantIds(request == null ? null : request.tenantIds());
        String signatureHash = request == null ? null : request.signatureHash();
        if (signatureHash == null || signatureHash.isBlank()) {
            throw new ValidationException("signature_hash is required");
        }
        TenantContext signer = tenantAuthorizationService.requireBatchClosingSigner(authorizationHeader, tenantIds);

        List<BatchAccountingPeriodSignatureResult> results = new ArrayList<>();
        int signed = 0;
        int skipped = 0;
        int failed = 0;

        for (String tenantId : tenantIds) {
            try {
                try {
                    AccountingPeriodClosing existing = fiscalAccountingService.closing(tenantId, periodMonth);
                    results.add(new BatchAccountingPeriodSignatureResult(
                            tenantId,
                            "SKIPPED",
                            "already_signed",
                            existing
                    ));
                    skipped++;
                    continue;
                } catch (NotFoundException ignored) {
                    // Open period: continue and sign below.
                }

                AccountingPeriodClosing closing = fiscalAccountingService.signClosing(
                        tenantId,
                        periodMonth,
                        signer,
                        signatureHash + ":" + tenantId
                );
                results.add(new BatchAccountingPeriodSignatureResult(
                        tenantId,
                        "SIGNED",
                        "signed",
                        closing
                ));
                signed++;
            } catch (RuntimeException exception) {
                results.add(new BatchAccountingPeriodSignatureResult(
                        tenantId,
                        "ERROR",
                        exception.getMessage() == null ? "batch_sign_failed" : exception.getMessage(),
                        null
                ));
                failed++;
            }
        }

        return new BatchAccountingPeriodSignatureResponse(
                periodMonth.toString(),
                tenantIds.size(),
                signed,
                skipped,
                failed,
                results
        );
    }

    @GET
    @Path("/tenants/{tenantId}/profit/available")
    @Operation(summary = "Consultar lucro disponivel", description = "Consolida lucro liberado por fechamentos assinados, retiradas registradas e saldo disponivel para distribuicao.")
    @SecurityRequirement(name = "bearerAuth")
    public ProfitAvailability profitAvailability(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.profitAvailability(tenantId);
    }

    @GET
    @Path("/tenants/{tenantId}/profit/distributions")
    @Operation(summary = "Listar distribuicoes de lucro", description = "Lista retiradas/distribuicoes registradas para o tenant, opcionalmente filtradas por mes fechado.")
    @SecurityRequirement(name = "bearerAuth")
    public List<ProfitDistribution> profitDistributions(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("month") String month) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return fiscalAccountingService.profitDistributions(tenantId, month == null || month.isBlank() ? null : parseMonth(month));
    }

    @POST
    @Path("/tenants/{tenantId}/profit/distributions")
    @Operation(summary = "Registrar distribuicao de lucro", description = "Registra uma retirada contra um periodo contabil assinado e abate o saldo disponivel.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ProfitDistributionRequest.class)))
    public Response createProfitDistribution(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            ProfitDistributionRequest request) {
        TenantContext actor = tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("profit_distribution is required");
        }
        ProfitDistribution distribution = fiscalAccountingService.createProfitDistribution(new CreateProfitDistributionCommand(
                tenantId,
                parseRequiredMonth(request.periodMonth()),
                request.amount(),
                request.distributedAt(),
                request.recipientName(),
                request.notes(),
                actor.userId(),
                actor.email()
        ));
        return Response.status(Response.Status.CREATED).entity(distribution).build();
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
        ReportEntryStatus status = parseStatus(request.status());
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
                status,
                request.releaseDate(),
                request.buyerName(),
                request.invoiceNumber()
        ));
        reconcileStockMovements(request, entry);
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

    private void reconcileStockMovements(ReportEntryIngestRequest request, ReportEntry entry) {
        if (entry == null) {
            return;
        }
        if (isSaleStatus(entry.status())) {
            recordStockExits(request, entry);
            return;
        }
        if (isReversalStatus(entry.status())) {
            stockService.reverseSaleMovements(entry.tenantId(), entry.orderId(), entry.saleDate());
        }
    }

    private void recordStockExits(ReportEntryIngestRequest request, ReportEntry entry) {
        if (!isSaleStatus(entry.status())) {
            return;
        }

        Map<String, BigDecimal> quantitiesBySku = new LinkedHashMap<>();
        for (ReportEntryItemRequest item : request.items()) {
            if (item == null) {
                continue;
            }
            String sku = normalizeSku(item.sku());
            BigDecimal quantity = positiveQuantity(item.quantity());
            if (sku == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            quantitiesBySku.merge(sku, quantity, BigDecimal::add);
        }

        quantitiesBySku.forEach((sku, quantity) ->
                stockService.recordSaleMovement(entry.tenantId(), sku, quantity, entry.orderId(), entry.saleDate()));
    }

    private boolean isSaleStatus(ReportEntryStatus status) {
        return status == ReportEntryStatus.PAID
                || status == ReportEntryStatus.PENDING_RELEASE
                || status == ReportEntryStatus.RECEIVED;
    }

    private boolean isReversalStatus(ReportEntryStatus status) {
        return status == ReportEntryStatus.CANCELLED
                || status == ReportEntryStatus.REFUNDED;
    }

    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim();
    }

    private BigDecimal positiveQuantity(BigDecimal quantity) {
        return quantity == null ? BigDecimal.ZERO : quantity;
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

    private YearMonth parseRequiredMonth(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("period_month is required");
        }
        return parseMonth(value);
    }

    private List<String> normalizeTenantIds(List<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            throw new ValidationException("tenant_ids is required");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String tenantId : tenantIds) {
            if (tenantId != null && !tenantId.isBlank()) {
                unique.add(tenantId.trim());
            }
        }
        if (unique.isEmpty()) {
            throw new ValidationException("tenant_ids is required");
        }
        if (unique.size() > 100) {
            throw new ValidationException("tenant_ids_limit_exceeded");
        }
        return List.copyOf(unique);
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
            @JsonProperty("items") List<ReportEntryItemRequest> items,
            @JsonProperty("invoice_number") String invoiceNumber) {
        public ReportEntryIngestRequest {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    @Schema(name = "ReportEntryItemRequest")
    public record ReportEntryItemRequest(
            @JsonProperty("sku") String sku,
            @JsonProperty("title") String title,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("unit_value") BigDecimal unitValue,
            @JsonProperty("gross_value") BigDecimal grossValue) {
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

    @Schema(name = "BatchAccountingPeriodSignatureRequest")
    public record BatchAccountingPeriodSignatureRequest(
            @JsonProperty("tenant_ids") List<String> tenantIds,
            @JsonProperty("signature_hash") String signatureHash) {
    }

    @Schema(name = "BatchAccountingPeriodSignatureResponse")
    public record BatchAccountingPeriodSignatureResponse(
            @JsonProperty("period_month") String periodMonth,
            @JsonProperty("requested_count") int requestedCount,
            @JsonProperty("signed_count") int signedCount,
            @JsonProperty("skipped_count") int skippedCount,
            @JsonProperty("failed_count") int failedCount,
            @JsonProperty("results") List<BatchAccountingPeriodSignatureResult> results) {
    }

    @Schema(name = "BatchAccountingPeriodSignatureResult")
    public record BatchAccountingPeriodSignatureResult(
            @JsonProperty("tenant_id") String tenantId,
            @JsonProperty("status") String status,
            @JsonProperty("message") String message,
            @JsonProperty("closing") AccountingPeriodClosing closing) {
    }

    @Schema(name = "ProfitDistributionRequest")
    public record ProfitDistributionRequest(
            @JsonProperty("period_month") String periodMonth,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("distributed_at") LocalDate distributedAt,
            @JsonProperty("recipient_name") String recipientName,
            @JsonProperty("notes") String notes) {
    }

    @Schema(name = "DreCalculationRequest")
    public record DreCalculationRequest(
            @JsonProperty("from") LocalDate from,
            @JsonProperty("to") LocalDate to) {
    }

    @GET
    @Path("/tenants/{tenantId}/invoices")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Listar NF-es rastreadas", description = "Retorna NF-es registradas para o tenant no periodo.")
    public List<InvoiceEntry> getInvoices(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        tenantAuthorizationService.requireReadable(authHeader, tenantId);
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return invoiceTrackingService.listByPeriod(tenantId, fromDate, toDate);
    }

    @GET
    @Path("/tenants/{tenantId}/invoices/unmatched")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "NF-es sem lancamento", description = "Retorna NF-es que nao possuem lancamento correspondente no periodo (nao reconciliadas).")
    public List<InvoiceEntry> getUnmatchedInvoices(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        tenantAuthorizationService.requireReadable(authHeader, tenantId);
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return invoiceTrackingService.listUnmatched(tenantId, fromDate, toDate);
    }

    // ─── Estoque / CMV ────────────────────────────────────────────────────────

    @GET
    @Path("/tenants/{tenantId}/stock/items")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Listar itens do estoque")
    public List<StockItemResponse> listStockItems(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(auth, tenantId);
        return stockService.listItems(tenantId).stream()
                .map(StockItemResponse::from)
                .toList();
    }

    @POST
    @Path("/tenants/{tenantId}/stock/items")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Cadastrar ou atualizar item do estoque")
    public StockItemResponse upsertStockItem(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            StockItemRequest request) {
        tenantAuthorizationService.requireWritable(auth, tenantId);
        return StockItemResponse.from(stockService.upsertStockItem(tenantId, request.sku(), request.description(), request.unitCost()));
    }

    @POST
    @Path("/tenants/{tenantId}/stock/nfe-import")
    @Consumes("text/xml")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Importar NF-e de fornecedor (XML)", description = "Faz upload do XML da NF-e do fornecedor para alimentar o estoque com custo de aquisicao.")
    public PurchaseEntry importNfeXml(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            String xmlContent) {
        tenantAuthorizationService.requireWritable(auth, tenantId);
        return stockService.importNfeXml(tenantId, xmlContent);
    }

    @GET
    @Path("/tenants/{tenantId}/stock/purchases")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Listar entradas de NF-e de fornecedores")
    public List<PurchaseEntry> listPurchaseEntries(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        tenantAuthorizationService.requireReadable(auth, tenantId);
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return stockService.listPurchaseEntries(tenantId, fromDate, toDate);
    }

    // ─── Extrato Bancário / OFX ───────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/bank/ofx-import")
    @Consumes(MediaType.TEXT_PLAIN)
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Importar extrato bancário OFX", description = "Importa extrato da conta PJ no formato OFX para categorizar despesas financeiras automaticamente.")
    public List<BankTransaction> importOfx(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            String ofxContent) {
        tenantAuthorizationService.requireWritable(auth, tenantId);
        return bankTransactionService.importOfx(tenantId, ofxContent);
    }

    @GET
    @Path("/tenants/{tenantId}/bank/transactions")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Listar transações bancárias importadas")
    public List<BankTransaction> listBankTransactions(
            @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to) {
        tenantAuthorizationService.requireReadable(auth, tenantId);
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return bankTransactionService.listByPeriod(tenantId, fromDate, toDate);
    }

    @Schema(name = "StockItemResponse")
    public record StockItemResponse(
            @JsonProperty("id") String id,
            @JsonProperty("tenant_id") String tenantId,
            @JsonProperty("sku") String sku,
            @JsonProperty("description") String description,
            @JsonProperty("unit_cost") BigDecimal unitCost,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("created_at") LocalDateTime createdAt,
            @JsonProperty("updated_at") LocalDateTime updatedAt) {
        static StockItemResponse from(StockItem item) {
            return new StockItemResponse(
                    item.id(),
                    item.tenantId(),
                    item.sku(),
                    item.description(),
                    item.unitCost(),
                    item.quantity(),
                    item.createdAt(),
                    item.updatedAt());
        }
    }

    @Schema(name = "StockItemRequest")
    public record StockItemRequest(
            @JsonProperty("sku") String sku,
            @JsonProperty("description") String description,
            @JsonProperty("unit_cost") BigDecimal unitCost) {
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
