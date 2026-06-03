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
public class CoreServiceClient {

    private static final Logger LOG = Logger.getLogger(CoreServiceClient.class);
    private static final String SERVICE = "core-service";

    @ConfigProperty(name = "ai.client.core-service.base-url",
            defaultValue = "http://core-service:8080")
    String baseUrl;

    @ConfigProperty(name = "ai.client.internal-token")
    String internalToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String listConnectors(String tenantId) {
        String url = baseUrl + "/core/connectors";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalToken)
                    .header("X-Tenant-Id", tenantId)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ServiceClientException(SERVICE, response.statusCode(),
                        "core_query_failed: " + response.statusCode());
            }
            return response.body();
        } catch (ServiceClientException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warnf("core-service call failed: %s", ex.getMessage());
            throw new ServiceClientException(SERVICE, 503, "core_service_unavailable", ex);
        }
    }
}
