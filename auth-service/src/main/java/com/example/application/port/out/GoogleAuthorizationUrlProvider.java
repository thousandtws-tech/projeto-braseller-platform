package com.example.application.port.out;

import java.util.Optional;

public interface GoogleAuthorizationUrlProvider {
    Optional<String> authorizationUrl();
}
