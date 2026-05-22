package com.example.application.service;

import com.example.application.exception.FeatureNotConfiguredException;
import com.example.application.port.out.GoogleAuthorizationUrlProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GoogleOAuthService {
    @Inject
    GoogleAuthorizationUrlProvider googleAuthorizationUrlProvider;

    public String authorizationUrl() {
        return googleAuthorizationUrlProvider.authorizationUrl()
                .orElseThrow(() -> new FeatureNotConfiguredException("google_oauth_not_configured"));
    }
}
