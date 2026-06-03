package com.example.infrastructure.security;

import com.example.application.port.out.InternalServiceAuthorizer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConfiguredInternalServiceAuthorizer implements InternalServiceAuthorizer {

    @ConfigProperty(name = "ai.internal-token")
    String internalToken;

    @Override
    public boolean isAuthorized(String token) {
        return internalToken != null && internalToken.equals(token);
    }
}
