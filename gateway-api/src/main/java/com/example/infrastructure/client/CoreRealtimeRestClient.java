package com.example.infrastructure.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.SseEvent;

@Path("/core/connectors")
@RegisterRestClient(configKey = "core-realtime")
public interface CoreRealtimeRestClient {
    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<SseEvent<String>> events(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Last-Event-ID") String lastEventId,
            @QueryParam("cursor") long cursor);
}
