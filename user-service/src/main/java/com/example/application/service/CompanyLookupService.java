package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.domain.model.CompanyLookupView;
import com.example.infrastructure.client.ApiIntegrationEventRequest;
import com.example.infrastructure.client.CoreIntegrationsRestClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class CompanyLookupService {
    private static final Logger LOG = Logger.getLogger(CompanyLookupService.class);
    private static final String INTEGRATION_NAME = "brasil_api";
    private static final String SYSTEM_TENANT_ID = "__system__";

    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    CoreIntegrationsRestClient coreIntegrationsRestClient;

    @ConfigProperty(name = "user.internal-token")
    String internalToken;

    @ConfigProperty(name = "user.company-lookup.cnpj.base-url")
    String cnpjLookupBaseUrl;

    @ConfigProperty(name = "user.company-lookup.timeout-ms")
    long timeoutMs;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public Optional<CompanyLookupView> lookupCnpj(String cnpj) {
        String digits = onlyDigits(cnpj);
        if (digits.length() != 14) {
            throw new ValidationException("invalid_cnpj");
        }

        long startNanos = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/" + URLEncoder.encode(digits, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseTimeMs = elapsedMs(startNanos);
            if (response.statusCode() == 404) {
                recordEvent(responseTimeMs, response.statusCode(), "SUCCESS", null, "INFO", null, null, null);
                return Optional.empty();
            }
            if (response.statusCode() == 400) {
                recordEvent(responseTimeMs, response.statusCode(), "SUCCESS", null, "INFO", null, null, null);
                throw new ValidationException("invalid_cnpj");
            }
            if (response.statusCode() != 200) {
                recordEvent(responseTimeMs, response.statusCode(), "FAILURE", "UNAVAILABLE", "CRITICAL",
                        "Consulta de CNPJ na BrasilAPI indisponivel", "circuit_breaker_evaluated",
                        "http_status_" + response.statusCode());
                throw new ValidationException("company_lookup_unavailable");
            }
            recordEvent(responseTimeMs, response.statusCode(), "SUCCESS", null, "INFO", null, null, null);
            BrazilApiCnpjResponse company = objectMapper.readValue(response.body(), BrazilApiCnpjResponse.class);
            return Optional.of(toView(company));
        } catch (InterruptedException exception) {
            recordEvent(elapsedMs(startNanos), null, "FAILURE", "TIMEOUT", "WARNING",
                    "Consulta de CNPJ na BrasilAPI interrompida", "retry_scheduled", exception.getMessage());
            Thread.currentThread().interrupt();
            throw new ValidationException("company_lookup_unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            recordEvent(elapsedMs(startNanos), null, "FAILURE", "TIMEOUT", "WARNING",
                    "Consulta de CNPJ na BrasilAPI temporariamente indisponivel", "retry_scheduled", exception.getMessage());
            throw new ValidationException("company_lookup_unavailable");
        }
    }

    private int elapsedMs(long startNanos) {
        return (int) Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private void recordEvent(
            int responseTimeMs,
            Integer httpStatus,
            String outcome,
            String failureType,
            String severity,
            String impact,
            String actionTaken,
            String errorMessage) {
        try (jakarta.ws.rs.core.Response response = coreIntegrationsRestClient.recordEvent(internalToken, new ApiIntegrationEventRequest(
                SYSTEM_TENANT_ID,
                INTEGRATION_NAME,
                "/api/cnpj/v1/{cnpj}",
                "lookup_cnpj",
                Instant.now(),
                responseTimeMs,
                httpStatus,
                outcome,
                failureType,
                severity,
                impact,
                actionTaken,
                errorMessage
        ))) {
            response.bufferEntity();
        } catch (RuntimeException exception) {
            LOG.warnf(exception, "Failed to record api integration event for %s", INTEGRATION_NAME);
        }
    }

    private CompanyLookupView toView(BrazilApiCnpjResponse company) {
        return new CompanyLookupView(
                onlyDigits(company.cnpj()),
                trimToNull(company.legalName()),
                trimToNull(company.tradeName()),
                company.cnaeCode() == null ? null : String.valueOf(company.cnaeCode()),
                trimToNull(company.cnaeDescription()),
                trimToNull(joinStreet(company.streetType(), company.street())),
                trimToNull(company.number()),
                trimToNull(company.complement()),
                trimToNull(company.neighborhood()),
                trimToNull(company.city()),
                trimToNull(company.state()),
                zipCode(company.zipCode()),
                trimToNull(company.registrationStatus()),
                trimToNull(company.email()),
                trimToNull(company.phone()),
                "BRASIL_API_MINHA_RECEITA"
        );
    }

    private String joinStreet(String streetType, String street) {
        if (isBlank(streetType)) {
            return street;
        }
        if (isBlank(street)) {
            return streetType;
        }
        return streetType.trim() + " " + street.trim();
    }

    private String zipCode(Integer value) {
        if (value == null) {
            return null;
        }
        return String.format("%08d", value);
    }

    private String baseUrl() {
        return cnpjLookupBaseUrl.endsWith("/")
                ? cnpjLookupBaseUrl.substring(0, cnpjLookupBaseUrl.length() - 1)
                : cnpjLookupBaseUrl;
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrazilApiCnpjResponse(
            String cnpj,
            @JsonProperty("razao_social") String legalName,
            @JsonProperty("nome_fantasia") String tradeName,
            @JsonProperty("cnae_fiscal") Integer cnaeCode,
            @JsonProperty("cnae_fiscal_descricao") String cnaeDescription,
            @JsonProperty("descricao_tipo_de_logradouro") String streetType,
            @JsonProperty("logradouro") String street,
            @JsonProperty("numero") String number,
            @JsonProperty("complemento") String complement,
            @JsonProperty("bairro") String neighborhood,
            @JsonProperty("municipio") String city,
            @JsonProperty("uf") String state,
            @JsonProperty("cep") Integer zipCode,
            @JsonProperty("descricao_situacao_cadastral") String registrationStatus,
            String email,
            @JsonProperty("ddd_telefone_1") String phone
    ) {
    }
}
