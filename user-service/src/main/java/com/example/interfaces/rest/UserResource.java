package com.example.interfaces.rest;

import com.example.application.command.GrantAccountantAccessCommand;
import com.example.application.command.RegisterTenantCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.command.VerifyPasswordCommand;
import com.example.application.exception.ConflictException;
import com.example.application.exception.ForbiddenException;
import com.example.application.exception.ValidationException;
import com.example.application.service.CompanyLookupService;
import com.example.application.service.TenantAuthorizationService;
import com.example.application.service.UserIdentityService;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.AccountantClientView;
import com.example.domain.model.CompanyLookupView;
import com.example.domain.model.IdentityVerification;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.TenantContext;
import com.example.domain.model.UserView;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "Tenants, usuarios, papeis e acesso secundario do contador.")
public class UserResource {
    @Inject
    UserIdentityService userIdentityService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    CompanyLookupService companyLookupService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do user-service", description = "Verifica se o user-service esta respondendo.")
    @APIResponse(responseCode = "200", description = "Servico em execucao.")
    public String status() {
        return "User Service is running";
    }

    @POST
    @Path("/tenants/register")
    @Operation(summary = "Registrar tenant", description = "Cria um tenant isolado e o usuario administrador inicial com papeis ADMIN e VENDEDOR.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RegisterTenantRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Tenant e usuario criados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RegisteredTenant.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "409", description = "Conflito ao cadastrar tenant/usuario.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response registerTenant(RegisterTenantRequest request) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userIdentityService.registerTenant(new RegisterTenantCommand(
                            request.legalName(),
                            request.tradeName(),
                            request.adminName(),
                            request.email(),
                            request.password(),
                            request.cnpj(),
                            request.cnaeCode(),
                            request.cnaeDescription(),
                            request.addressStreet(),
                            request.addressNumber(),
                            request.addressComplement(),
                            request.addressNeighborhood(),
                            request.addressCity(),
                            request.addressState(),
                            request.addressZipCode()
                    )))
                    .build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (ConflictException exception) {
            return Response.status(Response.Status.CONFLICT).entity(new RestError(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/tenants/{tenantId}/accountants")
    @Operation(summary = "Conceder acesso ao contador", description = "Cria acesso secundario do contador com papel CONTADOR e perfil somente leitura.")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = GrantAccountantAccessRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Acesso do contador criado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AccountantAccessView.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido ou tenant/usuario inexistente.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "403", description = "Papel ADMIN exigido para conceder acesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response grantAccountantAccess(
            @Parameter(description = "Bearer JWT emitido pelo auth-service.", required = true)
            @HeaderParam("Authorization") String authorizationHeader,
            @Parameter(description = "Tenant que concedera o acesso.", required = true)
            @PathParam("tenantId") String tenantId,
            GrantAccountantAccessRequest request) {
        try {
            TenantContext context = tenantAuthorizationService.requireAdmin(authorizationHeader, tenantId);
            String fullName = (request.firstName() + " " + request.lastName()).trim();
            return Response.status(Response.Status.CREATED)
                    .entity(userIdentityService.grantAccountantAccess(new GrantAccountantAccessCommand(
                            tenantId,
                            request.email(),
                            request.firstName(),
                            request.lastName(),
                            fullName,
                            request.temporaryPassword(),
                            context.userId()
                    )))
                    .build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @GET
    @Path("/tenants/{tenantId}/members")
    @Operation(summary = "Listar membros do tenant", description = "Retorna usuarios e papeis do tenant resolvido pelo JWT.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Membros retornados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserView.class))),
            @APIResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "403", description = "JWT de outro tenant ou papel insuficiente.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public List<UserView> listTenantMembers(
            @Parameter(description = "Bearer JWT emitido pelo auth-service.", required = true)
            @HeaderParam("Authorization") String authorizationHeader,
            @Parameter(description = "Tenant consultado.", required = true)
            @PathParam("tenantId") String tenantId) {
        tenantAuthorizationService.requireTenant(authorizationHeader, tenantId);
        return userIdentityService.listTenantMembers(tenantId);
    }

    @GET
    @Path("/company-lookup/cnpj/{cnpj}")
    @Operation(summary = "Consultar empresa por CNPJ", description = "Busca dados cadastrais basicos na BrasilAPI/Minha Receita para preencher o cadastro automaticamente.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Empresa encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CompanyLookupView.class))),
            @APIResponse(responseCode = "400", description = "CNPJ invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "404", description = "CNPJ nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response lookupCompanyByCnpj(@PathParam("cnpj") String cnpj) {
        try {
            return companyLookupService.lookupCnpj(cnpj)
                    .map(company -> Response.ok(company).build())
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                            .entity(new RestError("cnpj_not_found"))
                            .build());
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @GET
    @Path("/accountant/clients")
    @Operation(summary = "Listar clientes do contador", description = "Retorna todos os tenants com acesso ativo para o contador autenticado, habilitando painel BPO multi-cliente.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clientes do contador retornados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AccountantClientView.class))),
            @APIResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "403", description = "Papel CONTADOR exigido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public List<AccountantClientView> listAccountantClients(
            @Parameter(description = "Bearer JWT emitido pelo auth-service.", required = true)
            @HeaderParam("Authorization") String authorizationHeader) {
        TenantContext context = tenantAuthorizationService.requireAuthenticated(authorizationHeader);
        return userIdentityService.listAccountantClients(context);
    }

    @POST
    @Path("/internal/identity/verify-password")
    @Operation(summary = "Validar senha internamente", description = "Endpoint interno usado pelo auth-service para validar credenciais no user-service.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = VerifyPasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Credenciais validas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityVerification.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "401", description = "Credenciais invalidas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "403", description = "Token interno invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response verifyPassword(@HeaderParam("X-Internal-Token") String providedInternalToken,
                                   VerifyPasswordRequest request) {
        try {
            return userIdentityService.verifyPassword(
                            providedInternalToken,
                            new VerifyPasswordCommand(request.email(), request.password())
                    )
                    .map(this::ok)
                    .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                            .entity(new RestError("invalid_credentials"))
                            .build());
        } catch (ForbiddenException exception) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RestError(exception.getMessage())).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/internal/identity/sync-profile")
    @Operation(summary = "Sincronizar perfil externo", description = "Endpoint interno usado pelo auth-service para persistir dados de perfil vindos de Keycloak/Google.")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SyncExternalProfileRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Perfil sincronizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserView.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "403", description = "Token interno invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "404", description = "Usuario ativo nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response syncExternalProfile(@HeaderParam("X-Internal-Token") String providedInternalToken,
                                        SyncExternalProfileRequest request) {
        try {
            return userIdentityService.syncExternalProfile(
                            providedInternalToken,
                            new SyncExternalProfileCommand(
                                    request.email(),
                                    request.provider(),
                                    request.providerSubject(),
                                    request.fullName(),
                                    request.preferredUsername(),
                                    request.firstName(),
                                    request.lastName(),
                                    request.pictureUrl(),
                                    request.emailVerified()
                            )
                    )
                    .map(this::ok)
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                            .entity(new RestError("user_not_found"))
                            .build());
        } catch (ForbiddenException exception) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RestError(exception.getMessage())).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    private Response ok(IdentityVerification verification) {
        return Response.ok(verification).build();
    }

    private Response ok(UserView user) {
        return Response.ok(user).build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new RestError(message)).build();
    }

    @Schema(name = "RegisterTenantRequest", description = "Dados para criar tenant e usuario administrador inicial.")
    public record RegisterTenantRequest(
            String legalName,
            String tradeName,
            String adminName,
            String email,
            String password,
            String cnpj,
            String cnaeCode,
            String cnaeDescription,
            String addressStreet,
            String addressNumber,
            String addressComplement,
            String addressNeighborhood,
            String addressCity,
            String addressState,
            String addressZipCode
    ) {
    }

    @Schema(name = "GrantAccountantAccessRequest", description = "Dados para conceder acesso secundario ao contador. firstName e lastName sao obrigatorios pelo Keycloak (realm brasaller).")
    public record GrantAccountantAccessRequest(
            String email,
            String firstName,
            String lastName,
            String temporaryPassword
    ) {}


    @Schema(name = "VerifyPasswordRequest", description = "Credenciais validadas internamente pelo auth-service.")
    public record VerifyPasswordRequest(String email, String password) {
    }

    @Schema(name = "SyncExternalProfileRequest", description = "Dados de perfil externo sincronizados pelo auth-service.")
    public record SyncExternalProfileRequest(String email, String provider, String providerSubject, String fullName,
                                             String preferredUsername, String firstName, String lastName,
                                             String pictureUrl, boolean emailVerified) {
    }
}
