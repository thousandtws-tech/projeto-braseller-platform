package com.example.application.port.out;

import java.util.Optional;

public interface KeycloakAuthorizationUrlProvider {
    Optional<String> authorizationUrl(String identityProviderHint);
}
