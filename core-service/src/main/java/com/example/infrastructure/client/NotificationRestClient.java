package com.example.infrastructure.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey = "notification-service")
public interface NotificationRestClient {
    @POST
    @Path("/notifications/events/new-sale")
    @Retry(
            maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS,
            jitter = 250, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {ProcessingException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 5, failureRatio = 0.6,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2
    )
    Response newSale(
            @HeaderParam("X-Internal-Token") String internalToken,
            NewSaleNotificationRequest request);

    @POST
    @Path("/notifications/events/api-integration-alert")
    @Retry(
            maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS,
            jitter = 250, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {ProcessingException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 5, failureRatio = 0.6,
            delay = 30, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2
    )
    Response apiIntegrationAlert(
            @HeaderParam("X-Internal-Token") String internalToken,
            ApiIntegrationAlertRequest request);
}
