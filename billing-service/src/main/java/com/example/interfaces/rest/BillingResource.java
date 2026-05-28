package com.example.interfaces.rest;

import com.example.application.command.BillingWebhookCommand;
import com.example.application.command.ChangePlanCommand;
import com.example.application.command.StartTrialCommand;
import com.example.application.exception.ValidationException;
import com.example.application.service.BillingService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.BillingPlan;
import com.example.domain.model.BillingPlanCode;
import com.example.domain.model.BillingProvider;
import com.example.domain.model.BillingSubscription;
import com.example.domain.model.BillingWebhookEventType;
import com.example.infrastructure.security.ConfiguredBillingWebhookAuthorizer;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/billing")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Billing", description = "Planos, trials, assinaturas e webhooks de cobranca.")
public class BillingResource {
    @Inject
    BillingService billingService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    ConfiguredBillingWebhookAuthorizer webhookAuthorizer;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do billing-service", description = "Verifica se o billing-service esta respondendo.")
    public String status() {
        return "Billing Service is running";
    }

    @GET
    @Path("/plans")
    @Operation(summary = "Listar planos", description = "Lista planos comerciais disponiveis: Basico, Pro e Agencia.")
    public List<BillingPlan> plans() {
        return billingService.plans();
    }

    @GET
    @Path("/tenants/{tenantId}/subscription")
    @Operation(summary = "Consultar assinatura", description = "Retorna plano, status, trial e acesso de cobranca do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public BillingSubscription subscription(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireReadable(authorizationHeader, tenantId);
        return billingService.subscription(tenantId);
    }

    @POST
    @Path("/tenants/{tenantId}/trial")
    @Operation(summary = "Iniciar trial", description = "Cria assinatura em trial de 14 dias para o plano escolhido.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = StartTrialRequest.class)))
    public Response startTrial(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            StartTrialRequest request) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        BillingSubscription subscription = billingService.startTrial(new StartTrialCommand(
                tenantId,
                request == null ? BillingPlanCode.BASIC : parsePlanCode(request.planCode())
        ));
        return Response.status(Response.Status.CREATED).entity(subscription).build();
    }

    @PUT
    @Path("/tenants/{tenantId}/subscription/plan")
    @Operation(summary = "Alterar plano", description = "Permite upgrade ou downgrade pelo proprio usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChangePlanRequest.class)))
    public BillingSubscription changePlan(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("tenantId") String tenantId,
            ChangePlanRequest request) {
        tenantAuthorizationService.requireWritable(authorizationHeader, tenantId);
        if (request == null) {
            throw new ValidationException("plan_change_request is required");
        }
        return billingService.changePlan(new ChangePlanCommand(
                tenantId,
                parsePlanCode(request.planCode())
        ));
    }

    @POST
    @Path("/webhooks")
    @Operation(summary = "Receber webhook de cobranca", description = "Aplica ativacao, pagamento, suspensao ou cancelamento vindos do provedor.")
    @SecurityRequirement(name = "billingWebhookToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BillingWebhookRequest.class)))
    public BillingSubscription webhook(
            @HeaderParam("X-Billing-Webhook-Token") String webhookToken,
            BillingWebhookRequest request) {
        webhookAuthorizer.requireWebhookToken(webhookToken);
        if (request == null) {
            throw new ValidationException("webhook_request is required");
        }
        return billingService.applyWebhook(new BillingWebhookCommand(
                BillingProvider.parseOrLocal(request.provider()),
                request.providerEventId(),
                BillingWebhookEventType.parse(request.eventType()),
                request.tenantId(),
                request.planCode() == null || request.planCode().isBlank() ? null : parsePlanCode(request.planCode()),
                request.providerCustomerId(),
                request.providerSubscriptionId(),
                request.reason(),
                request.payload()
        ));
    }

    private BillingPlanCode parsePlanCode(String value) {
        return BillingPlanCode.parse(value);
    }

    @Schema(name = "StartTrialRequest")
    public record StartTrialRequest(@JsonProperty("plan_code") String planCode) {
    }

    @Schema(name = "ChangePlanRequest")
    public record ChangePlanRequest(@JsonProperty("plan_code") String planCode) {
    }

    @Schema(name = "BillingWebhookRequest")
    public record BillingWebhookRequest(
            @JsonProperty("provider") String provider,
            @JsonProperty("provider_event_id") String providerEventId,
            @JsonProperty("event_type") String eventType,
            @JsonProperty("tenant_id") String tenantId,
            @JsonProperty("plan_code") String planCode,
            @JsonProperty("provider_customer_id") String providerCustomerId,
            @JsonProperty("provider_subscription_id") String providerSubscriptionId,
            @JsonProperty("reason") String reason,
            @JsonProperty("payload") String payload) {
    }
}
