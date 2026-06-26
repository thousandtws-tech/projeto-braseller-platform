package com.example.application.dto;

import com.example.domain.model.DownstreamRoute;

import java.util.List;
import java.util.Map;

public record DownstreamRequest(
        String method,
        DownstreamRoute route,
        String remainingPath,
        Map<String, List<String>> queryParameters,
        Map<String, List<String>> headers,
        String body) {

    public DownstreamRequest {
        if (route == null) {
            throw new IllegalArgumentException("route is required");
        }
        remainingPath = remainingPath == null ? "" : remainingPath;
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String downstreamPath() {
        return route.resolveDownstreamPath(remainingPath);
    }
}
