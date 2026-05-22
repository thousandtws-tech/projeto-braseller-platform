package com.example.application.port.out;

import com.example.application.command.LoginCommand;
import com.example.application.command.RegisterCommand;
import com.example.domain.model.AuthIdentity;

import java.util.Optional;

public interface UserIdentityGateway {
    AuthIdentity registerTenant(RegisterCommand command);

    Optional<AuthIdentity> verifyPassword(LoginCommand command);
}
