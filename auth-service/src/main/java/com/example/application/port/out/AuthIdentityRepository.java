package com.example.application.port.out;

import com.example.domain.model.AuthIdentity;

import java.util.Optional;

public interface AuthIdentityRepository {
    AuthIdentity synchronize(AuthIdentity identity);

    Optional<AuthIdentity> findIdentityByEmail(String email);

    void linkProviderSubject(String email, String provider, String subject);
}
