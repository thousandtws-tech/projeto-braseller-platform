package com.example.application.dto;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record GatewayRequest(
        String method,
        String serviceSegment,
        String remainingPath,
        Map<String, List<String>> queryParameters,
        Map<String, List<String>> headers,
        String body) {

    public GatewayRequest {
        method = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        serviceSegment = serviceSegment == null ? "" : serviceSegment.trim();
        remainingPath = remainingPath == null ? "" : remainingPath.trim();
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
