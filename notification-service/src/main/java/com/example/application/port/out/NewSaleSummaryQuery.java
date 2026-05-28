package com.example.application.port.out;

import com.example.domain.model.TenantNewSaleSummary;

import java.util.Optional;

public interface NewSaleSummaryQuery {
    Optional<TenantNewSaleSummary> getTenantSummary(String tenantId);
}
