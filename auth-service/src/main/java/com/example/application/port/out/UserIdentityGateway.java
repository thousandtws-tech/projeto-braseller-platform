package com.example.application.port.out;

import com.example.application.command.LoginCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.EmailVerificationDispatch;

import java.util.Optional;

public interface UserIdentityGateway {
    AuthIdentity registerTenant(RegisterCommand command);

    Optional<AuthIdentity> verifyPassword(LoginCommand command);

    EmailVerificationDispatch resendEmailVerificationCode(String email);

    AuthIdentity verifyEmailCode(String email, String code);

    Optional<AuthIdentity> syncExternalProfile(SyncExternalProfileCommand command);

    Optional<AuthIdentity> findByEmail(String email);

    Optional<AuthIdentity> markEmailVerified(String email);

    Optional<AuthIdentity> resetPassword(String email, String newPassword);
}
