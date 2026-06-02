package com.example.infrastructure.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "notification-service")
public interface NotificationRestClient {
    @POST
    @Path("/notifications/events/new-sale")
    Response newSale(
            @HeaderParam("X-Internal-Token") String internalToken,
            NewSaleNotificationRequest request);
}
