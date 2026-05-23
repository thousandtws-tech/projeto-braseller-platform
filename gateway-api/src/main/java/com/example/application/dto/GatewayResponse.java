package com.example.application.dto;

import java.util.List;
import java.util.Map;

public record GatewayResponse(int status, String body, Map<String, List<String>> headers) {
    public GatewayResponse {
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
