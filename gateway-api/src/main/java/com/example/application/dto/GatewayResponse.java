package com.example.application.dto;

import java.util.List;
import java.util.Map;

public record GatewayResponse(int status, byte[] body, Map<String, List<String>> headers) {
    public GatewayResponse {
        body = body == null ? new byte[0] : body.clone();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public boolean hasBody() {
        return body.length > 0;
    }
}
