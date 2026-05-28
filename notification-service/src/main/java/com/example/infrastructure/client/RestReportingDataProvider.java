package com.example.infrastructure.client;

import com.example.application.port.out.ReportingDataProvider;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.ReportingFinancialSummary;
import com.example.infrastructure.client.dto.ReportingPaymentReleaseResponse;
import com.example.infrastructure.client.dto.ReportingSummaryResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RestReportingDataProvider implements ReportingDataProvider {
    @RestClient
    ReportingRestClient reportingRestClient;

    @ConfigProperty(name = "notification.reporting.internal-token")
    String internalToken;

    @Override
    public ReportingFinancialSummary summary(String tenantId, LocalDate from, LocalDate to) {
        ReportingSummaryResponse response = reportingRestClient.summary(internalToken, tenantId, from, to);
        if (response == null) {
            return ReportingFinancialSummary.empty();
        }
        return new ReportingFinancialSummary(response.grossValue(), response.entryCount());
    }

    @Override
    public List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to) {
        List<ReportingPaymentReleaseResponse> responses = reportingRestClient.paymentReleases(
                internalToken,
                tenantId,
                platform,
                from,
                to
        );
        if (responses == null) {
            return List.of();
        }
        return responses.stream()
                .map(response -> new PaymentReleaseAlert(
                        response.tenantId(),
                        response.platform(),
                        response.paymentId(),
                        response.amount(),
                        response.releaseDate()
                ))
                .toList();
    }
}
