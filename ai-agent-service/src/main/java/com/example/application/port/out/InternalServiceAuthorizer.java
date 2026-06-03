package com.example.application.port.out;

public interface InternalServiceAuthorizer {
    boolean isAuthorized(String token);
}
