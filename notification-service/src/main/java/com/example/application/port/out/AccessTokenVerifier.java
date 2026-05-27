package com.example.application.port.out;

import com.example.domain.model.TenantContext;

public interface AccessTokenVerifier {
    TenantContext verify(String authorizationHeader);
}
