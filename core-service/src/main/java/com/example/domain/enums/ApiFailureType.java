package com.example.domain.enums;

public enum ApiFailureType {
    AUTH_FAILURE,
    TOKEN_EXPIRED,
    RATE_LIMIT,
    TIMEOUT,
    UNAVAILABLE,
    PAYLOAD_CHANGE,
    UNKNOWN
}
