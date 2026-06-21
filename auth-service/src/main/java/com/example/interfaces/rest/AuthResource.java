package com.example.interfaces.rest;

import com.example.application.command.LoginCommand;
import com.example.application.command.RefreshTokenCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.command.RequestEmailVerificationCommand;
import com.example.application.command.RequestPasswordResetCommand;
import com.example.application.command.ResetPasswordCommand;
import com.example.application.command.ValidatePasswordResetCommand;
import com.example.application.command.VerifyEmailCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.FeatureNotConfiguredException;
import com.example.application.exception.IdentityGatewayException;
import com.example.application.exception.ValidationException;
import com.example.application.service.AuthChallengeService;
import com.example.application.service.AuthenticationService;
import com.example.application.service.KeycloakOAuthService;
import com.example.domain.model.AuthChallengeAccepted;
import com.example.domain.model.AuthTokenSet;
import com.example.domain.model.CodeValidationResult;
import com.example.domain.model.RegistrationAccepted;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
@Tag(name = "Auth", description = "Cadastro, login, refresh token, logout e OAuth via Keycloak.")
public class AuthResource {
    @Inject
    AuthenticationService authenticationService;

    @Inject
    KeycloakOAuthService keycloakOAuthService;

    @Inject
    AuthChallengeService authChallengeService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do auth-service", description = "Verifica se o auth-service esta respondendo.")
    @APIResponse(responseCode = "200", description = "Servico em execucao.")
    public String status() {
        return "Auth Service is running";
    }

    @POST
    @Path("/register")
    @Operation(summary = "Cadastrar vendedor", description = "Cria tenant/usuario pendente, cria o usuario no Keycloak e envia codigo de verificacao de e-mail.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "202", description = "Cadastro criado e aguardando verificacao de e-mail.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RegistrationAccepted.class))),
            @APIResponse(responseCode = "400", description = "Payload invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "409", description = "E-mail/tenant em conflito.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "502", description = "Falha ao comunicar com o user-service.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response register(RegisterRequest request) {
        try {
            return Response.status(Response.Status.ACCEPTED).entity(authenticationService.register(new RegisterCommand(
                    request.tenantName(),
                    request.fullName(),
                    request.email(),
                    request.password(),
                    request.cnpj(),
                    request.legalName(),
                    request.tradeName(),
                    request.cnaeCode(),
                    request.cnaeDescription(),
                    request.addressStreet(),
                    request.addressNumber(),
                    request.addressComplement(),
                    request.addressNeighborhood(),
                    request.addressCity(),
                    request.addressState(),
                    request.addressZipCode()
            ))).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/email-verification/request")
    @Operation(summary = "Solicitar codigo de verificacao de e-mail", description = "Envia codigo de verificacao quando o e-mail existe e esta pendente. A resposta e generica.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EmailRequest.class)))
    public Response requestEmailVerification(@HeaderParam("X-Forwarded-For") String forwardedFor,
                                             EmailRequest request) {
        try {
            AuthChallengeAccepted accepted = authChallengeService.requestEmailVerification(
                    new RequestEmailVerificationCommand(request.email(), clientIp(forwardedFor)));
            return Response.accepted(accepted).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/email-verification/verify")
    @Operation(summary = "Validar codigo de verificacao de e-mail", description = "Consome codigo valido, marca usuario como ativo/verificado e sincroniza Keycloak.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CodeRequest.class)))
    public Response verifyEmail(CodeRequest request) {
        try {
            authChallengeService.verifyEmail(new VerifyEmailCommand(request.email(), request.code()));
            return Response.ok(new SimpleMessage("email_verified")).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/password-reset/request")
    @Operation(summary = "Solicitar recuperacao de senha", description = "Envia codigo de redefinicao quando o e-mail existe e esta ativo. A resposta e generica.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EmailRequest.class)))
    public Response requestPasswordReset(@HeaderParam("X-Forwarded-For") String forwardedFor,
                                         EmailRequest request) {
        try {
            AuthChallengeAccepted accepted = authChallengeService.requestPasswordReset(
                    new RequestPasswordResetCommand(request.email(), clientIp(forwardedFor)));
            return Response.accepted(accepted).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/password-reset/validate")
    @Operation(summary = "Validar codigo de recuperacao", description = "Confirma se o codigo de recuperacao ainda e valido sem consumi-lo.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CodeRequest.class)))
    public Response validatePasswordReset(CodeRequest request) {
        try {
            CodeValidationResult result = authChallengeService.validatePasswordReset(
                    new ValidatePasswordResetCommand(request.email(), request.code()));
            return Response.ok(result).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/password-reset/reset")
    @Operation(summary = "Redefinir senha", description = "Consome codigo valido e redefine senha local/Keycloak.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
    public Response resetPassword(ResetPasswordRequest request) {
        try {
            authChallengeService.resetPassword(
                    new ResetPasswordCommand(request.email(), request.code(), request.newPassword()));
            return Response.ok(new SimpleMessage("password_reset")).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login", description = "Autentica e-mail/senha no Keycloak e emite access token da plataforma + refresh token Keycloak.")
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
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
        }
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Renovar access token", description = "Renova a sessao no Keycloak e emite novo access token da plataforma.")
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
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout", description = "Revoga a sessao/refresh token no Keycloak.")
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
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        }
    }

    @GET
    @Path("/oauth/google/authorize-url")
    @Operation(summary = "Gerar URL OAuth Google", description = "Retorna a URL de autorizacao do Keycloak com kc_idp_hint=google.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "URL de autorizacao gerada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GoogleAuthorizeUrlResponse.class))),
            @APIResponse(responseCode = "501", description = "OAuth Keycloak/Google nao configurado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response googleAuthorizeUrl() {
        try {
            return Response.ok(new GoogleAuthorizeUrlResponse(keycloakOAuthService.googleAuthorizationUrl())).build();
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/oauth/google/callback")
    @Operation(summary = "Login/cadastro OAuth Google", description = "Troca no Keycloak o code recebido pelo broker Google. Se o e-mail ja existir, faz login; se nao existir, cria tenant/usuario usando tenantName.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = GoogleCallbackRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Login ou cadastro Google realizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthTokenSet.class))),
            @APIResponse(responseCode = "401", description = "Code ou identidade Google invalida.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class))),
            @APIResponse(responseCode = "501", description = "OAuth Keycloak/Google nao configurado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response googleCallback(GoogleCallbackRequest request) {
        try {
            return Response.ok(keycloakOAuthService.googleCallback(request.code(), request.tenantName())).build();
        } catch (FeatureNotConfiguredException exception) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new RestError(exception.getMessage())).build();
        } catch (ValidationException exception) {
            return badRequest(exception.getMessage());
        } catch (AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(exception.getMessage())).build();
        } catch (IdentityGatewayException exception) {
            return identityGatewayFailure(exception);
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

    private String clientIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        return forwardedFor.split(",", 2)[0].trim();
    }

    @Schema(name = "AuthRegisterRequest", description = "Dados para criar tenant e usuario administrador inicial.")
    public record RegisterRequest(
            String tenantName,
            String fullName,
            String email,
            String password,
            String cnpj,
            String legalName,
            String tradeName,
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

    @Schema(name = "AuthLoginRequest", description = "Credenciais de e-mail/senha autenticadas no Keycloak.")
    public record LoginRequest(String email, String password) {
    }

    @Schema(name = "AuthEmailRequest", description = "E-mail usado em fluxos de verificacao e recuperacao.")
    public record EmailRequest(String email) {
    }

    @Schema(name = "AuthCodeRequest", description = "E-mail e codigo de uso unico.")
    public record CodeRequest(String email, String code) {
    }

    @Schema(name = "AuthResetPasswordRequest", description = "Codigo de recuperacao e nova senha.")
    public record ResetPasswordRequest(String email, String code, String newPassword) {
    }

    @Schema(name = "AuthSimpleMessage", description = "Resposta simples de sucesso.")
    public record SimpleMessage(String message) {
    }

    @Schema(name = "AuthRefreshRequest", description = "Refresh token emitido pelo Keycloak e retornado pelo auth-service.")
    public record RefreshRequest(String refreshToken) {
    }

    @Schema(name = "AuthLogoutResponse", description = "Resultado da tentativa de revogacao de refresh token.")
    public record LogoutResponse(boolean revoked) {
    }

    @Schema(name = "GoogleAuthorizeUrlResponse", description = "URL para iniciar OAuth Google.")
    public record GoogleAuthorizeUrlResponse(String authorizeUrl) {
    }

    @Schema(name = "GoogleCallbackRequest", description = "Code retornado pelo broker Google do Keycloak. tenantName e obrigatorio apenas para primeiro cadastro.")
    public record GoogleCallbackRequest(String code, String tenantName) {
    }

}
