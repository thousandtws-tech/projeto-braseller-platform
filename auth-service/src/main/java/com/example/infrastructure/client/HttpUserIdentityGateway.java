package com.example.infrastructure.client;

import com.example.application.command.LoginCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.exception.IdentityGatewayException;
import com.example.application.exception.TransientIdentityGatewayException;
import com.example.application.port.out.UserIdentityGateway;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import java.time.temporal.ChronoUnit;
import com.example.domain.model.AuthIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "auth.user-service.url")
    String userServiceUrl;

    @ConfigProperty(name = "auth.user-service.internal-token")
    String internalToken;

    @ConfigProperty(name = "auth.http-client.connect-timeout-ms")
    long connectTimeoutMs;

    @ConfigProperty(name = "auth.http-client.request-timeout-ms")
    long requestTimeoutMs;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = {TransientIdentityGatewayException.class}
    )
    @Fallback(fallbackMethod = "registerTenantFallback")
    public AuthIdentity registerTenant(RegisterCommand command) {
        UserTenantRegistrationRequest userRequest = new UserTenantRegistrationRequest(
                firstNonBlank(command.legalName(), command.tenantName()).trim(),
                firstNonBlank(command.tradeName(), command.tenantName()).trim(),
                command.fullName().trim(),
                command.email().trim(),
                command.password(),
                blankToNull(command.cnpj()),
                blankToNull(command.cnaeCode()),
                blankToNull(command.cnaeDescription()),
                blankToNull(command.addressStreet()),
                blankToNull(command.addressNumber()),
                blankToNull(command.addressComplement()),
                blankToNull(command.addressNeighborhood()),
                blankToNull(command.addressCity()),
                blankToNull(command.addressState()),
                blankToNull(command.addressZipCode())
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
    @Retry(
            maxRetries = 2, delay = 400, delayUnit = ChronoUnit.MILLIS,
            jitter = 200, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {TransientIdentityGatewayException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = {TransientIdentityGatewayException.class}
    )
    @Fallback(fallbackMethod = "verifyPasswordFallback")
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
    @Retry(
            maxRetries = 2, delay = 400, delayUnit = ChronoUnit.MILLIS,
            jitter = 200, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {TransientIdentityGatewayException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = {TransientIdentityGatewayException.class}
    )
    @Fallback(fallbackMethod = "syncExternalProfileFallback")
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

    private AuthIdentity registerTenantFallback(RegisterCommand command) {
        throw new IdentityGatewayException(503, "user_service_unavailable");
    }

    private Optional<AuthIdentity> verifyPasswordFallback(LoginCommand command) {
        throw new IdentityGatewayException(503, "user_service_unavailable");
    }

    private Optional<AuthIdentity> syncExternalProfileFallback(SyncExternalProfileCommand command) {
        throw new IdentityGatewayException(503, "user_service_unavailable");
    }

    private HttpResponse<String> send(String path, Object body, boolean internal) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (internal) {
                request.header("X-Internal-Token", internalToken);
            }
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransientIdentityGatewayException(503, "user_service_unavailable", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new TransientIdentityGatewayException(503, "user_service_unavailable", exception);
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

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
                user.emailVerified(),
                user.accountantTenantIds()
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
                verification.emailVerified(),
                verification.accountantTenantIds()
        );
    }

    public record UserTenantRegistrationRequest(
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
                               String provider, String providerSubject, String status, List<String> roles,
                               List<String> accountantTenantIds) {
    }

    public record IdentityVerificationResponse(String userId, String tenantId, String email, String fullName,
                                               String preferredUsername, String firstName, String lastName,
                                               String pictureUrl, boolean emailVerified, String provider,
                                               String providerSubject, List<String> roles, List<String> accountantTenantIds) {
    }

    public record ErrorResponse(String message) {
    }
}
