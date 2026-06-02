package com.example.infrastructure.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "reporting-service")
public interface ReportingRestClient {
    @POST
    @Path("/reports/internal/entries")
    Response ingest(
            @HeaderParam("X-Internal-Token") String internalToken,
            ReportEntryIngestRequest request);
}
