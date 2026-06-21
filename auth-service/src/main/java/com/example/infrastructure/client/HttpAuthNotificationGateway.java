package com.example.infrastructure.client;

import com.example.application.exception.TransientIdentityGatewayException;
import com.example.application.port.out.AuthNotificationGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class HttpAuthNotificationGateway implements AuthNotificationGateway {
    private static final Logger LOG = Logger.getLogger(HttpAuthNotificationGateway.class);
    private static final DateTimeFormatter EXPIRATION_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "auth.notification-service.url")
    String notificationServiceUrl;

    @ConfigProperty(name = "auth.notification-service.internal-token")
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
            requestVolumeThreshold = 5, failureRatio = 0.6,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            failOn = {TransientIdentityGatewayException.class}
    )
    @Fallback(fallbackMethod = "emailVerificationFallback")
    public void sendEmailVerificationCode(String email, String code, Instant expiresAt) {
        String subject = "Confirme seu e-mail no BraSeller";
        String message = """
                Ola,

                Use o codigo %s para confirmar seu e-mail no BraSeller.

                O codigo expira em %s e pode ser usado apenas uma vez. Se voce nao criou uma conta, ignore esta mensagem.
                """.formatted(code, EXPIRATION_FORMATTER.format(expiresAt));
        send(new AuthEmailRequest(email, subject, message, "EMAIL_VERIFICATION"));
    }

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 5, failureRatio = 0.6,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            failOn = {TransientIdentityGatewayException.class}
    )
    @Fallback(fallbackMethod = "passwordResetFallback")
    public void sendPasswordResetCode(String email, String code, Instant expiresAt) {
        String subject = "Redefinicao de senha do BraSeller";
        String message = """
                Ola,

                Recebemos uma solicitacao para redefinir sua senha no BraSeller. Use o codigo %s para criar uma nova senha.

                O codigo expira em %s e pode ser usado apenas uma vez. Se voce nao solicitou esta alteracao, ignore esta mensagem.
                """.formatted(code, EXPIRATION_FORMATTER.format(expiresAt));
        send(new AuthEmailRequest(email, subject, message, "PASSWORD_RESET"));
    }

    private void send(AuthEmailRequest requestBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/notifications/events/auth-email"))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", internalToken)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TransientIdentityGatewayException(response.statusCode(), "notification_service_unavailable");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransientIdentityGatewayException(503, "notification_service_unavailable", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new TransientIdentityGatewayException(503, "notification_service_unavailable", exception);
        }
    }

    private void emailVerificationFallback(String email, String code, Instant expiresAt) {
        LOG.warnf("Email verification notification could not be delivered to %s; code ending: %s",
                maskEmail(email), suffix(code));
    }

    private void passwordResetFallback(String email, String code, Instant expiresAt) {
        LOG.warnf("Password reset notification could not be delivered to %s; code ending: %s",
                maskEmail(email), suffix(code));
    }

    private String baseUrl() {
        return notificationServiceUrl.endsWith("/")
                ? notificationServiceUrl.substring(0, notificationServiceUrl.length() - 1)
                : notificationServiceUrl;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] pieces = email.split("@", 2);
        String local = pieces[0].isBlank() ? "*" : pieces[0].substring(0, 1) + "***";
        return local + "@" + pieces[1];
    }

    private String suffix(String code) {
        return code == null || code.length() < 2 ? "**" : code.substring(code.length() - 2);
    }

    public record AuthEmailRequest(String recipientEmail, String subject, String message, String purpose) {
    }
}
