package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorConnectionStatus {
    ACTIVE("active"),
    EXPIRED("expired"),
    DISCONNECTED("disconnected"),
    UNAVAILABLE("unavailable");

    private final String value;

    ConnectorConnectionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
