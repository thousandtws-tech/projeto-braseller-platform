package com.example.infrastructure.monitoring;

import com.example.domain.enums.ApiFailureType;

public record ApiCallContext(
        String tenantId,
        String integrationName,
        String endpoint,
        String operation,
        ApiFailureType failureTypeHint) {

    public static ApiCallContext of(String tenantId, String integrationName, String endpoint, String operation) {
        return new ApiCallContext(tenantId, integrationName, endpoint, operation, null);
    }

    public ApiCallContext withFailureTypeHint(ApiFailureType failureTypeHint) {
        return new ApiCallContext(tenantId, integrationName, endpoint, operation, failureTypeHint);
    }
}
