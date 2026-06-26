package com.example.infrastructure.client;

import com.example.application.dto.DownstreamRequest;
import com.example.application.dto.GatewayResponse;
import com.example.application.exception.DownstreamServiceUnavailableException;
import com.example.application.port.out.DownstreamServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.time.temporal.ChronoUnit;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@ApplicationScoped
public class RestClientDownstreamServiceClient implements DownstreamServiceClient {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "content-length",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    @RestClient
    DownstreamRestClient restClient;

    @Override
    @CircuitBreaker(
            requestVolumeThreshold = 20, failureRatio = 0.5,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            failOn = {DownstreamServiceUnavailableException.class, ProcessingException.class}
    )
    @Fallback(fallbackMethod = "exchangeFallback")
    public GatewayResponse exchange(DownstreamRequest request) {
        URI targetUri = buildTargetUri(request);
        try (Response response = send(request, targetUri)) {
            return new GatewayResponse(
                    response.getStatus(),
                    response.hasEntity() ? response.readEntity(byte[].class) : new byte[0],
                    copyResponseHeaders(response.getStringHeaders())
            );
        } catch (ProcessingException exception) {
            throw new DownstreamServiceUnavailableException(
                    request.route().serviceName(),
                    exception
            );
        }
    }

    private GatewayResponse exchangeFallback(DownstreamRequest request) {
        throw new DownstreamServiceUnavailableException(
                request.route().serviceName(), new RuntimeException("circuit_breaker_open")
        );
    }

    private Response send(DownstreamRequest request, URI targetUri) {
        String target = targetUri.toString();
        String authorization = firstHeader(request, "authorization");
        return switch (request.method()) {
            case "GET" -> restClient.get(target, authorization);
            case "POST" -> restClient.post(target, authorization, request.body());
            case "PUT" -> restClient.put(target, authorization, request.body());
            case "PATCH" -> restClient.patch(target, authorization, request.body());
            case "DELETE" -> restClient.delete(target, authorization);
            default -> throw new IllegalStateException("Unsupported HTTP method already validated: " + request.method());
        };
    }

    private String firstHeader(DownstreamRequest request, String headerName) {
        return request.headers().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(null);
    }

    private URI buildTargetUri(DownstreamRequest request) {
        UriBuilder builder = UriBuilder.fromUri(request.route().baseUri() + request.downstreamPath());
        request.queryParameters().forEach((name, values) -> values.forEach(value -> builder.queryParam(name, value)));
        return builder.build();
    }

    private Map<String, List<String>> copyResponseHeaders(MultivaluedMap<String, String> headers) {
        Map<String, List<String>> copiedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                copiedHeaders.put(name, List.copyOf(values));
            }
        });
        return copiedHeaders;
    }
}
