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
public class UserServiceClient {

    private static final Logger LOG = Logger.getLogger(UserServiceClient.class);
    private static final String SERVICE = "user-service";

    @ConfigProperty(name = "ai.client.user-service.base-url",
            defaultValue = "http://user-service:8080")
    String baseUrl;

    @ConfigProperty(name = "ai.client.internal-token")
    String internalToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String getMembers(String tenantId) {
        String url = baseUrl + "/users/tenants/" + tenantId + "/members";
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
                        "user_query_failed: " + response.statusCode());
            }
            return response.body();
        } catch (ServiceClientException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warnf("user-service call failed: %s", ex.getMessage());
            throw new ServiceClientException(SERVICE, 503, "user_service_unavailable", ex);
        }
    }
}
