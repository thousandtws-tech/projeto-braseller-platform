package com.example.infrastructure.client;

import com.example.infrastructure.client.dto.ReportingPaymentReleaseResponse;
import com.example.infrastructure.client.dto.ReportingSummaryResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDate;
import java.util.List;

@RegisterRestClient(configKey = "reporting-service")
public interface ReportingRestClient {
    @GET
    @Path("/reports/internal/tenants/{tenantId}/summary")
    ReportingSummaryResponse summary(
            @HeaderParam("X-Internal-Token") String internalToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to);

    @GET
    @Path("/reports/internal/tenants/{tenantId}/payment-releases")
    List<ReportingPaymentReleaseResponse> paymentReleases(
            @HeaderParam("X-Internal-Token") String internalToken,
            @PathParam("tenantId") String tenantId,
            @QueryParam("platform") String platform,
            @QueryParam("from") LocalDate from,
            @QueryParam("to") LocalDate to);
}
