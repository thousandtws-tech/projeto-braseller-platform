package com.example.infrastructure.mail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.example.application.port.out.EmailVerificationSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailerEmailVerificationSender implements EmailVerificationSender {
    private static final Logger LOG = Logger.getLogger(MailerEmailVerificationSender.class);

    @Inject
    Mailer mailer;

    @Inject
    Template emailVerification;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "user.mail.from")
    String from;

    @ConfigProperty(name = "user.internal-token")
    String internalToken;

    @ConfigProperty(name = "notification.service.url")
    Optional<String> notificationServiceUrl;

    @ConfigProperty(name = "quarkus.mailer.mock")
    boolean mailerMock;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void send(String tenantId, String recipientEmail, String recipientName, String code, Instant expiresAt) {
        if (notificationServiceUrl.isPresent() && !notificationServiceUrl.get().isBlank()) {
            sendThroughNotificationService(tenantId, recipientEmail, recipientName, code, expiresAt);
            return;
        }

        String html = emailVerification
                .data("name", recipientName == null || recipientName.isBlank() ? recipientEmail : recipientName)
                .data("email", recipientEmail)
                .data("code", code)
                .data("expiresAt", expiresAt)
                .render();

        mailer.send(Mail.withHtml(recipientEmail, "Codigo de verificacao Brasaller", html)
                .setFrom(from)
                .setText("Seu codigo de verificacao Brasaller e " + code));

        if (mailerMock) {
            LOG.infof("email_verification_code_mock recipient=%s code=%s expiresAt=%s", recipientEmail, code, expiresAt);
        }
    }

    private void sendThroughNotificationService(
            String tenantId,
            String recipientEmail,
            String recipientName,
            String code,
            Instant expiresAt) {
        String baseUrl = notificationServiceUrl.orElseThrow().replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/notifications/events/email-verification"))
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", internalToken)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(new EmailVerificationNotificationRequest(
                        tenantId,
                        recipientEmail,
                        recipientName,
                        code,
                        expiresAt
                ))))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("notification_service_email_verification_failed_status_" + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("notification_service_email_verification_io_failure", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("notification_service_email_verification_interrupted", exception);
        }
    }

    private String toJson(EmailVerificationNotificationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("notification_service_email_verification_payload_serialization_failed", exception);
        }
    }

    private record EmailVerificationNotificationRequest(
            String tenantId,
            String recipientEmail,
            String recipientName,
            String code,
            Instant expiresAt) {
    }
}
