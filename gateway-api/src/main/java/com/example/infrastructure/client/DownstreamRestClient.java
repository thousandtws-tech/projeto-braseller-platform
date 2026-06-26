package com.example.infrastructure.client;

import io.quarkus.rest.client.reactive.Url;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient(configKey = "gateway-downstream")
@RegisterClientHeaders
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public interface DownstreamRestClient {
    @GET
    Response get(@Url String targetUri, @HeaderParam("Authorization") String authorization);

    @POST
    Response post(@Url String targetUri, @HeaderParam("Authorization") String authorization, String body);

    @PUT
    Response put(@Url String targetUri, @HeaderParam("Authorization") String authorization, String body);

    @PATCH
    Response patch(@Url String targetUri, @HeaderParam("Authorization") String authorization, String body);

    @DELETE
    Response delete(@Url String targetUri, @HeaderParam("Authorization") String authorization);
}
