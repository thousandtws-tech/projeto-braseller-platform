package com.example.interfaces.rest;

import com.example.application.exception.ConnectorValidationException;
import com.example.application.service.ConnectorService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.TenantContext;
import com.example.domain.model.connector.ConnectorDescriptor;
import com.example.domain.model.connector.ConnectorStatus;
import com.example.domain.model.connector.ConnectorToken;
import com.example.domain.model.connector.FeeInfo;
import com.example.domain.model.connector.InvoiceInfo;
import com.example.domain.model.connector.OrderStatus;
import com.example.domain.model.connector.PaymentInfo;
import com.example.domain.model.connector.StandardOrder;
import com.example.domain.model.connector.SyncAccepted;
import com.example.domain.model.connector.SyncJob;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Path("/core/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Connectors", description = "Contrato padronizado entre Core e conectores de marketplaces.")
public class ConnectorResource {
    @Inject
    ConnectorService connectorService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @GET
    @Operation(summary = "Listar conectores", description = "Lista conectores registrados sem expor implementacoes especificas ao Core.")
    @APIResponse(responseCode = "200", description = "Conectores registrados.")
    public List<ConnectorDescriptor> list() {
        return connectorService.list();
    }

    @POST
    @Path("/{connectorName}/authenticate")
    @Operation(summary = "Autenticar conector", description = "Executa authenticate() do conector pelo nome.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AuthenticateRequest.class)))
    public ConnectorToken authenticate(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            AuthenticateRequest request) {
        TenantContext context = tenantAuthorizationService.requireWritable(authorizationHeader);
        return connectorService.authenticate(connectorName, context.tenantId(), request.credentials());
    }

    @POST
    @Path("/{connectorName}/refresh-token")
    @Operation(summary = "Renovar token", description = "Renova usando o refresh token criptografado armazenado no Core.")
    @SecurityRequirement(name = "bearerAuth")
    public ConnectorToken refreshToken(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName) {
        TenantContext context = tenantAuthorizationService.requireWritable(authorizationHeader);
        return connectorService.refreshToken(connectorName, context.tenantId());
    }

    @GET
    @Path("/{connectorName}/orders")
    @Operation(summary = "Listar pedidos padronizados", description = "Executa getOrders(filtros) e retorna sempre o formato padronizado de pedido.")
    @SecurityRequirement(name = "bearerAuth")
    public List<StandardOrder> orders(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("status") String status,
            @QueryParam("limit") Integer limit) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getOrders(connectorName, context.tenantId(), from, to, parseStatus(status), limit);
    }

    @GET
    @Path("/{connectorName}/orders/{orderId}")
    @Operation(summary = "Detalhar pedido", description = "Executa getOrderDetail(id) do conector pelo nome.")
    @SecurityRequirement(name = "bearerAuth")
    public StandardOrder orderDetail(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            @PathParam("orderId") String orderId) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getOrderDetail(connectorName, context.tenantId(), orderId);
    }

    @GET
    @Path("/{connectorName}/orders/{orderId}/payments")
    @Operation(summary = "Consultar pagamentos", description = "Executa getPayments(orderId) do conector pelo nome.")
    @SecurityRequirement(name = "bearerAuth")
    public List<PaymentInfo> payments(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            @PathParam("orderId") String orderId) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getPayments(connectorName, context.tenantId(), orderId);
    }

    @GET
    @Path("/{connectorName}/orders/{orderId}/fees")
    @Operation(summary = "Consultar taxas", description = "Executa getFees(orderId) do conector pelo nome.")
    @SecurityRequirement(name = "bearerAuth")
    public List<FeeInfo> fees(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            @PathParam("orderId") String orderId) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getFees(connectorName, context.tenantId(), orderId);
    }

    @GET
    @Path("/{connectorName}/invoices")
    @Operation(summary = "Consultar notas fiscais", description = "Executa getInvoices(filtros) quando o conector suportar.")
    @SecurityRequirement(name = "bearerAuth")
    public List<InvoiceInfo> invoices(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to,
            @QueryParam("limit") Integer limit) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getInvoices(connectorName, context.tenantId(), from, to, limit);
    }

    @POST
    @Path("/{connectorName}/sync-all")
    @Operation(summary = "Enfileirar sincronizacao", description = "Cria um job em banco para executar syncAll(desde) do conector em background.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SyncAllRequest.class)))
    public Response syncAll(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName,
            SyncAllRequest request) {
        TenantContext context = tenantAuthorizationService.requireWritable(authorizationHeader);
        SyncAccepted accepted = connectorService.requestSyncAll(
                connectorName,
                context.tenantId(),
                context.email(),
                request == null ? null : request.since()
        );
        return Response.accepted(accepted).build();
    }

    @GET
    @Path("/sync-jobs/{jobId}")
    @Operation(summary = "Consultar job de sincronizacao", description = "Retorna status e resultado do processamento assincrono de syncAll.")
    @SecurityRequirement(name = "bearerAuth")
    public SyncJob syncJob(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("jobId") String jobId) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.syncJob(context.tenantId(), jobId);
    }

    @GET
    @Path("/{connectorName}/status")
    @Operation(summary = "Consultar status", description = "Executa getStatus() do conector pelo nome.")
    @SecurityRequirement(name = "bearerAuth")
    public ConnectorStatus status(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("connectorName") String connectorName) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return connectorService.getStatus(connectorName, context.tenantId());
    }

    public record AuthenticateRequest(Map<String, String> credentials) {
    }

    public record SyncAllRequest(Instant since) {
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.fromValue(status);
        } catch (IllegalArgumentException exception) {
            throw new ConnectorValidationException(exception.getMessage());
        }
    }
}
