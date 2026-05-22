package com.example.interfaces.rest;

import com.example.application.command.LoginCommand;
import com.example.application.command.RefreshTokenCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.FeatureNotConfiguredException;
import com.example.application.exception.IdentityGatewayException;
import com.example.application.exception.ValidationException;
import com.example.application.service.AuthenticationService;
import com.example.application.service.GoogleOAuthService;
import com.example.domain.model.AuthTokenSet;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Cadastro, login, refresh token, logout e OAuth.")
public class AuthResource {
    @Inject
    AuthenticationService authenticationService;

    @Inject
    GoogleOAuthService googleOAuthService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do auth-service", description = "Verifica se o auth-service esta respondendo.")
    @APIResponse(responseCode = "200", description = "Servico em execucao.")
    public String status() {
        return "Auth Service is running";
    }

    @POST
    @Path("/register")
    @Operation(summary = "Cadastrar vendedor", description = "Cria tenant e usuario admin/vendedor no user-service e emite tokens JWT.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cadastro realizado e tokens emitidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthTokenSet.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "409", description = "E-mail/tenant em conflito.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "502", description = "Falha ao comunicar com o user-service.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response register(RegisterRequest request) {
        try {
            return Response.ok(authenticationService.register(new RegisterCommand(
                    request.tenantName(),
                    request.fullName(),
                    request.email(),
                    request.password()
            ))).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login com e-mail e senha", description = "Valida credenciais no user-service e emite access token + refresh token.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoginRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Login realizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthTokenSet.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "401", description = "Credenciais invalidas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "502", description = "Falha ao comunicar com o user-service.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response login(LoginRequest request) {
        try {
            return Response.ok(authenticationService.login(new LoginCommand(request.email(), request.password()))).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Renovar access token", description = "Emite novo access token usando um refresh token valido e nao revogado.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RefreshRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Token renovado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthTokenSet.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "401", description = "Refresh token invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response refresh(RefreshRequest request) {
        try {
            return Response.ok(authenticationService.refresh(new RefreshTokenCommand(request.refreshToken()))).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token informado.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RefreshRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Resultado da revogacao.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogoutResponse.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response logout(RefreshRequest request) {
        try {
            return Response.ok(new LogoutResponse(authenticationService.logout(new RefreshTokenCommand(request.refreshToken())))).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @GET
    @Path("/oauth/google/authorize-url")
    @Operation(summary = "Gerar URL OAuth Google", description = "Retorna a URL de autorizacao Google quando GOOGLE_CLIENT_ID estiver configurado.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "URL de autorizacao gerada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GoogleAuthorizeUrlResponse.class))),
            @APIResponse(responseCode = "501", description = "OAuth Google nao configurado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response googleAuthorizeUrl() {
        try {
            return Response.ok(new GoogleAuthorizeUrlResponse(googleOAuthService.authorizationUrl())).build();
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        }
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new RestError(message)).build();
    }

    private Response identityGatewayFailure(IdentityGatewayException exception) {
        if (exception.status() == 409) {
            return Response.status(Response.Status.CONFLICT).entity(new RestError(exception.getMessage())).build();
        }
        if (exception.status() == 401) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        }
        if (exception.status() >= 500) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(new RestError(exception.getMessage())).build();
        }
        return badRequest(exception.getMessage());
    }

    @Schema(name = "AuthRegisterRequest", description = "Dados para criar tenant e usuario administrador inicial.")
    public record RegisterRequest(String tenantName, String fullName, String email, String password) {
    }

    @Schema(name = "AuthLoginRequest", description = "Credenciais de e-mail e senha.")
    public record LoginRequest(String email, String password) {
    }

    @Schema(name = "AuthRefreshRequest", description = "Refresh token emitido pelo auth-service.")
    public record RefreshRequest(String refreshToken) {
    }

    @Schema(name = "AuthLogoutResponse", description = "Resultado da tentativa de revogacao de refresh token.")
    public record LogoutResponse(boolean revoked) {
    }

    @Schema(name = "GoogleAuthorizeUrlResponse", description = "URL para iniciar OAuth Google.")
    public record GoogleAuthorizeUrlResponse(String authorizeUrl) {
    }
}
