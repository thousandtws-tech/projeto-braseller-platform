package com.example.application.port.out;

import com.example.domain.model.AuthIdentity;
import com.example.domain.model.KeycloakIdentity;
import com.example.domain.model.KeycloakTokenResponse;

public interface KeycloakOAuthClient {
    KeycloakTokenResponse exchangeCode(String code);

    KeycloakTokenResponse passwordGrant(String email, String password);

    KeycloakTokenResponse refresh(String refreshToken);

    boolean logout(String refreshToken);

    KeycloakIdentity userInfo(String accessToken);

    void createPasswordUser(AuthIdentity identity, String password);

    void synchronizeUser(AuthIdentity identity, String keycloakSubject);
}
