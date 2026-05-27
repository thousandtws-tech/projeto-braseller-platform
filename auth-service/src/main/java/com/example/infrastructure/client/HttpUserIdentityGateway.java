package com.example.infrastructure.client;

import com.example.application.command.LoginCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.exception.IdentityGatewayException;
import com.example.application.port.out.UserIdentityGateway;
import com.example.domain.model.AuthIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HttpUserIdentityGateway implements UserIdentityGateway {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "auth.user-service.url")
    String userServiceUrl;

    @ConfigProperty(name = "auth.user-service.internal-token")
    String internalToken;

    @Override
    public AuthIdentity registerTenant(RegisterCommand command) {
        UserTenantRegistrationRequest userRequest = new UserTenantRegistrationRequest(
                command.tenantName().trim(),
                command.tenantName().trim(),
                command.fullName().trim(),
                command.email().trim(),
                command.password()
        );
        HttpResponse<String> response = send("/users/tenants/register", userRequest, false);
        if (response.statusCode() != 201) {
            throw userServiceError("Could not register tenant", response.statusCode(), response.body());
        }

        RegisteredTenantResponse registeredTenant = read(response.body(), RegisteredTenantResponse.class);
        UserResponse adminUser = registeredTenant.adminUser();
        return toAuthIdentity(adminUser);
    }

    @Override
    public Optional<AuthIdentity> verifyPassword(LoginCommand command) {
        VerifyPasswordRequest userRequest = new VerifyPasswordRequest(command.email().trim(), command.password());
        HttpResponse<String> response = send("/users/internal/identity/verify-password", userRequest, true);
        if (response.statusCode() == 401) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw userServiceError("Could not verify identity", response.statusCode(), response.body());
        }

        IdentityVerificationResponse verification = read(response.body(), IdentityVerificationResponse.class);
        return Optional.of(toAuthIdentity(verification));
    }

    @Override
    public Optional<AuthIdentity> syncExternalProfile(SyncExternalProfileCommand command) {
        UserProfileSyncRequest userRequest = new UserProfileSyncRequest(
                command.email(),
                command.provider(),
                command.providerSubject(),
                command.fullName(),
                command.preferredUsername(),
                command.firstName(),
                command.lastName(),
                command.pictureUrl(),
                command.emailVerified()
        );
        HttpResponse<String> response = send("/users/internal/identity/sync-profile", userRequest, true);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw userServiceError("Could not synchronize external profile", response.statusCode(), response.body());
        }

        UserResponse user = read(response.body(), UserResponse.class);
        return Optional.of(toAuthIdentity(user));
    }

    private HttpResponse<String> send(String path, Object body, boolean internal) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (internal) {
                request.header("X-Internal-Token", internalToken);
            }
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IdentityGatewayException(503, "user_service_unavailable", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IdentityGatewayException(503, "user_service_unavailable", exception);
        }
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException exception) {
            throw new IdentityGatewayException(502, "invalid_user_service_response", exception);
        }
    }

    private IdentityGatewayException userServiceError(String fallback, int status, String body) {
        String message = fallback;
        try {
            ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
            if (error.message() != null && !error.message().isBlank()) {
                message = error.message();
            }
        } catch (IOException ignored) {
            if (body != null && !body.isBlank()) {
                message = body;
            }
        }
        return new IdentityGatewayException(status, message);
    }

    private String baseUrl() {
        return userServiceUrl.endsWith("/") ? userServiceUrl.substring(0, userServiceUrl.length() - 1) : userServiceUrl;
    }

    private AuthIdentity toAuthIdentity(UserResponse user) {
        return new AuthIdentity(
                user.id(),
                user.tenantId(),
                user.id(),
                user.email(),
                user.fullName(),
                user.roles(),
                user.status(),
                user.provider(),
                user.providerSubject(),
                user.preferredUsername(),
                user.firstName(),
                user.lastName(),
                user.pictureUrl(),
                user.emailVerified()
        );
    }

    private AuthIdentity toAuthIdentity(IdentityVerificationResponse verification) {
        return new AuthIdentity(
                verification.userId(),
                verification.tenantId(),
                verification.userId(),
                verification.email(),
                verification.fullName(),
                verification.roles(),
                "ACTIVE",
                verification.provider(),
                verification.providerSubject(),
                verification.preferredUsername(),
                verification.firstName(),
                verification.lastName(),
                verification.pictureUrl(),
                verification.emailVerified()
        );
    }

    public record UserTenantRegistrationRequest(String legalName, String tradeName, String adminName, String email, String password) {
    }

    public record VerifyPasswordRequest(String email, String password) {
    }

    public record UserProfileSyncRequest(String email, String provider, String providerSubject, String fullName,
                                         String preferredUsername, String firstName, String lastName, String pictureUrl,
                                         boolean emailVerified) {
    }

    public record RegisteredTenantResponse(TenantResponse tenant, UserResponse adminUser) {
    }

    public record TenantResponse(String id, String legalName, String tradeName, String status) {
    }

    public record UserResponse(String id, String tenantId, String email, String fullName, String preferredUsername,
                               String firstName, String lastName, String pictureUrl, boolean emailVerified,
                               String provider, String providerSubject, String status, List<String> roles) {
    }

    public record IdentityVerificationResponse(String userId, String tenantId, String email, String fullName,
                                               String preferredUsername, String firstName, String lastName,
                                               String pictureUrl, boolean emailVerified, String provider,
                                               String providerSubject, List<String> roles) {
    }

    public record ErrorResponse(String message) {
    }
}
