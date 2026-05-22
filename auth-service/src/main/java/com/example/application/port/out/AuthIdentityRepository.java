package com.example.application.port.out;

import com.example.domain.model.AuthIdentity;
import com.example.domain.model.AuthSession;

import java.util.Optional;

public interface AuthIdentityRepository {
    AuthIdentity synchronize(AuthIdentity identity);

    void createSession(AuthSession session);

    Optional<AuthIdentity> findIdentityByRefreshTokenHash(String refreshTokenHash);

    boolean revokeRefreshToken(String refreshTokenHash);
}
