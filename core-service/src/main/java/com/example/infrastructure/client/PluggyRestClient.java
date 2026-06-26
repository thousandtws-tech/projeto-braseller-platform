package com.example.infrastructure.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "pluggy-api")
public interface PluggyRestClient {
    @POST
    @Path("/auth")
    PluggyAuthResponse authenticate(PluggyAuthRequest request);

    @POST
    @Path("/connect_token")
    PluggyConnectTokenResponse createConnectToken(
            @HeaderParam("X-API-KEY") String apiKey,
            PluggyConnectTokenRequest request);
}
