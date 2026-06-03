package com.example.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class NotificationServiceClient {

    private static final Logger LOG = Logger.getLogger(NotificationServiceClient.class);
    private static final String SERVICE = "notification-service";

    @ConfigProperty(name = "ai.client.notification-service.base-url",
            defaultValue = "http://notification-service:8080")
    String baseUrl;

    @ConfigProperty(name = "ai.client.internal-token")
    String internalToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void sendEvent(String tenantId, String eventJson) {
        String url = baseUrl + "/notifications/events/agent-action";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", internalToken)
                    .header("X-Tenant-Id", tenantId)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(eventJson))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ServiceClientException(SERVICE, response.statusCode(),
                        "notification_event_failed: " + response.statusCode());
            }
        } catch (ServiceClientException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warnf("notification-service call failed: %s", ex.getMessage());
            throw new ServiceClientException(SERVICE, 503, "notification_service_unavailable", ex);
        }
    }
}
