package com.example.infrastructure.streaming;

import com.example.application.port.out.NewSaleSummaryQuery;
import com.example.domain.model.TenantNewSaleSummary;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
@IfBuildProfile("test")
public class TestNewSaleSummaryQuery implements NewSaleSummaryQuery {
    @Override
    public Optional<TenantNewSaleSummary> getTenantSummary(String tenantId) {
        return Optional.empty();
    }
}
