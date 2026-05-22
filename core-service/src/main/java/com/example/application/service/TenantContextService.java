package com.example.application.service;

import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TenantContextService {
    @Inject
    AccessTokenVerifier accessTokenVerifier;

    public TenantContext resolve(String authorizationHeader) {
        return accessTokenVerifier.verify(authorizationHeader);
    }
}
