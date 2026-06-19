package com.example.interfaces.rest;

import com.example.application.command.MonthlyClosingNotificationCommand;
import com.example.application.command.MlPaymentReleaseNotificationCommand;
import com.example.application.command.NewSaleNotificationCommand;
import com.example.application.command.UpdateNotificationPreferenceCommand;
import com.example.application.command.WeeklyAccountantReportCommand;
import com.example.application.command.EmailVerificationNotificationCommand;
import com.example.application.port.out.NewSaleSummaryQuery;
import com.example.application.service.NotificationService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.NotificationMessage;
import com.example.domain.model.NotificationPreference;
import com.example.domain.model.TenantContext;
import com.example.domain.model.TenantNewSaleSummary;
import com.example.infrastructure.security.ConfiguredInternalServiceAuthorizer;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Path("/notifications")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "Notificacoes, preferencias, alertas e e-mails automaticos.")
public class NotificationResource {
    @Inject
    NotificationService notificationService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    ConfiguredInternalServiceAuthorizer internalServiceAuthorizer;

    @Inject
    NewSaleSummaryQuery newSaleSummaryQuery;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do notification-service", description = "Verifica se o notification-service esta respondendo.")
    @APIResponse(responseCode = "200", description = "Servico em execucao.")
    public String status() {
        return "Notification Service is running";
    }

    @GET
    @Path("/tenants/{tenantId}/preferences")
    @Operation(summary = "Consultar preferencias", description = "Retorna as preferencias de notificacao do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public NotificationPreference preferences(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return notificationService.getPreference(tenantId);
    }

    @PUT
    @Path("/tenants/{tenantId}/preferences")
    @Operation(summary = "Atualizar preferencias", description = "Configura canais e tipos de notificacao por tenant.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PreferenceRequest.class)))
    public NotificationPreference updatePreferences(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            PreferenceRequest request) {
        TenantContext context = tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        return notificationService.updatePreference(new UpdateNotificationPreferenceCommand(
                tenantId,
                request.emailEnabled(),
                request.newSaleEnabled(),
                request.monthlyClosingEnabled(),
                request.mlPaymentReleaseEnabled(),
                request.weeklyAccountantReportEnabled(),
                firstNonBlank(request.recipientEmail(), context.email()),
                request.accountantEmail()
        ));
    }

    @GET
    @Path("/tenants/{tenantId}")
    @Operation(summary = "Listar notificacoes", description = "Lista notificacoes nao arquivadas do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public List<NotificationMessage> list(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @QueryParam("limit") Integer limit) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return notificationService.list(tenantId, limit);
    }

    @GET
    @Path("/tenants/{tenantId}/new-sale-summary")
    @Operation(summary = "Consultar resumo de vendas", description = "Retorna o agregado de novas vendas mantido no banco do notification-service.")
    @SecurityRequirement(name = "bearerAuth")
    public Response newSaleSummary(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        TenantNewSaleSummary summary = newSaleSummaryQuery.getTenantSummary(tenantId)
                .orElseGet(() -> TenantNewSaleSummary.empty(tenantId));
        return Response.ok(summary).build();
    }

    @PATCH
    @Path("/tenants/{tenantId}/{notificationId}/read")
    @Operation(summary = "Marcar como lida", description = "Marca uma notificacao do tenant como lida.")
    @SecurityRequirement(name = "bearerAuth")
    public Response markAsRead(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            @PathParam("notificationId") String notificationId) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        return notificationService.markAsRead(tenantId, notificationId)
                .map(notification -> Response.ok(notification).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).entity(new RestError("notification_not_found")).build());
    }

    @POST
    @Path("/tenants/{tenantId}/clear-read")
    @Operation(summary = "Arquivar lidas", description = "Arquiva notificacoes lidas do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public ClearReadResponse clearRead(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        return new ClearReadResponse(notificationService.archiveRead(tenantId));
    }

    @POST
    @Path("/events/email-verification")
    @Operation(summary = "Enviar codigo de verificacao de e-mail", description = "Dispara um e-mail transacional de verificacao para fluxo de cadastro.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EmailVerificationRequest.class)))
    public Response emailVerification(@HeaderParam("X-Internal-Token") String internalToken, EmailVerificationRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        NotificationMessage notification = notificationService.sendEmailVerification(new EmailVerificationNotificationCommand(
                request.tenantId(),
                request.recipientEmail(),
                request.recipientName(),
                request.code(),
                request.expiresAt()
        ));
        return Response.status(Response.Status.CREATED).entity(notification).build();
    }

    @POST
    @Path("/events/new-sale")
    @Operation(summary = "Notificar nova venda", description = "Cria notificacao de nova venda quando o usuario habilitou esse alerta.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NewSaleRequest.class)))
    public Response newSale(@HeaderParam("X-Internal-Token") String internalToken, NewSaleRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return optionalResponse(notificationService.notifyNewSale(new NewSaleNotificationCommand(
                request.eventId(),
                request.eventType(),
                request.occurredAt(),
                request.tenantId(),
                request.recipientEmail(),
                request.marketplace(),
                request.orderId(),
                request.amount()
        )));
    }

    @POST
    @Path("/events/ml-payment-release")
    @Operation(summary = "Alertar pagamento ML", description = "Cria alerta quando pagamento do Mercado Livre esta proximo de liberar.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = MlPaymentReleaseRequest.class)))
    public Response mlPaymentRelease(@HeaderParam("X-Internal-Token") String internalToken, MlPaymentReleaseRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return optionalResponse(notificationService.notifyMlPaymentRelease(new MlPaymentReleaseNotificationCommand(
                request.tenantId(),
                request.recipientEmail(),
                request.paymentId(),
                request.amount(),
                request.releaseDate()
        )));
    }

    @POST
    @Path("/events/monthly-closing")
    @Operation(summary = "Enviar fechamento mensal", description = "Envia e-mail automatico de fechamento mensal com resumo.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = MonthlyClosingRequest.class)))
    public Response monthlyClosing(@HeaderParam("X-Internal-Token") String internalToken, MonthlyClosingRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return optionalResponse(notificationService.sendMonthlyClosing(new MonthlyClosingNotificationCommand(
                request.tenantId(),
                request.recipientEmail(),
                request.period(),
                request.totalSales(),
                request.grossRevenue()
        )));
    }

    @POST
    @Path("/events/weekly-accountant-report")
    @Operation(summary = "Enviar relatorio semanal ao contador", description = "Envia relatorio semanal automatico ao contador configurado.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = WeeklyAccountantReportRequest.class)))
    public Response weeklyAccountantReport(@HeaderParam("X-Internal-Token") String internalToken, WeeklyAccountantReportRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        return optionalResponse(notificationService.sendWeeklyAccountantReport(new WeeklyAccountantReportCommand(
                request.tenantId(),
                request.accountantEmail(),
                request.weekStart(),
                request.weekEnd(),
                request.totalSales(),
                request.grossRevenue()
        )));
    }

    private Response optionalResponse(Optional<NotificationMessage> notification) {
        return notification
                .map(value -> Response.status(Response.Status.CREATED).entity(value).build())
                .orElseGet(() -> Response.status(Response.Status.ACCEPTED).entity(new SkippedResponse("notification_disabled")).build());
    }

    @Schema(name = "NotificationPreferenceRequest")
    public record PreferenceRequest(
            Boolean emailEnabled,
            Boolean newSaleEnabled,
            Boolean monthlyClosingEnabled,
            Boolean mlPaymentReleaseEnabled,
            Boolean weeklyAccountantReportEnabled,
            String recipientEmail,
            String accountantEmail) {
    }

    @Schema(name = "NewSaleNotificationRequest")
    public record NewSaleRequest(
            String eventId,
            String eventType,
            java.time.Instant occurredAt,
            String tenantId,
            String recipientEmail,
            String marketplace,
            String orderId,
            BigDecimal amount) {
    }

    @Schema(name = "EmailVerificationNotificationRequest")
    public record EmailVerificationRequest(
            String tenantId,
            String recipientEmail,
            String recipientName,
            String code,
            java.time.Instant expiresAt) {
    }

    @Schema(name = "MlPaymentReleaseNotificationRequest")
    public record MlPaymentReleaseRequest(String tenantId, String recipientEmail, String paymentId, BigDecimal amount, LocalDate releaseDate) {
    }

    @Schema(name = "MonthlyClosingNotificationRequest")
    public record MonthlyClosingRequest(String tenantId, String recipientEmail, YearMonth period, int totalSales, BigDecimal grossRevenue) {
    }

    @Schema(name = "WeeklyAccountantReportRequest")
    public record WeeklyAccountantReportRequest(
            String tenantId,
            String accountantEmail,
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalSales,
            BigDecimal grossRevenue) {
    }

    public record SkippedResponse(String reason) {
    }

    public record ClearReadResponse(int archivedCount) {
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }
}
