package com.example.application.service;

import com.example.application.command.RequestEmailVerificationCommand;
import com.example.application.command.RequestPasswordResetCommand;
import com.example.application.command.ResetPasswordCommand;
import com.example.application.command.ValidatePasswordResetCommand;
import com.example.application.command.VerifyEmailCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.AuthChallengeRepository;
import com.example.application.port.out.AuthIdentityRepository;
import com.example.application.port.out.AuthNotificationGateway;
import com.example.application.port.out.KeycloakOAuthClient;
import com.example.application.port.out.UserIdentityGateway;
import com.example.domain.model.AuthChallenge;
import com.example.domain.model.AuthChallengeAccepted;
import com.example.domain.model.AuthChallengeType;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.CodeValidationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class AuthChallengeService {
    private static final String GENERIC_RESET_MESSAGE =
            "Se o e-mail existir, enviaremos instrucoes para redefinir a senha.";
    private static final String GENERIC_VERIFICATION_MESSAGE =
            "Se o e-mail estiver pendente, enviaremos um codigo de verificacao.";

    @Inject
    AuthChallengeRepository challengeRepository;

    @Inject
    AuthIdentityRepository authIdentityRepository;

    @Inject
    UserIdentityGateway userIdentityGateway;

    @Inject
    KeycloakOAuthClient keycloakOAuthClient;

    @Inject
    AuthNotificationGateway notificationGateway;

    @ConfigProperty(name = "auth.challenges.code-secret")
    String codeSecret;

    @ConfigProperty(name = "auth.challenges.email-verification-ttl-minutes")
    long emailVerificationTtlMinutes;

    @ConfigProperty(name = "auth.challenges.password-reset-ttl-minutes")
    long passwordResetTtlMinutes;

    @ConfigProperty(name = "auth.challenges.max-attempts")
    int maxAttempts;

    @ConfigProperty(name = "auth.challenges.request-window-minutes")
    long requestWindowMinutes;

    @ConfigProperty(name = "auth.challenges.max-requests-per-window")
    int maxRequestsPerWindow;

    @ConfigProperty(name = "auth.challenges.fixed-code")
    Optional<String> fixedCode;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthChallengeAccepted requestEmailVerification(RequestEmailVerificationCommand command) {
        String email = requireEmail(command.email());
        String normalizedEmail = normalizeEmail(email);
        AuthChallengeType type = AuthChallengeType.EMAIL_VERIFICATION;
        Optional<AuthIdentity> identity = resolveIdentity(normalizedEmail);

        if (!isRateLimited(normalizedEmail, command.requestIp(), type)
                && identity.filter(this::needsEmailVerification).isPresent()) {
            issueChallenge(email, normalizedEmail, type, emailVerificationTtlMinutes, command.requestIp(), true);
        } else if (identity.isEmpty()) {
            recordNonDeliverableChallenge(email, normalizedEmail, type, emailVerificationTtlMinutes, command.requestIp());
        }

        return new AuthChallengeAccepted(GENERIC_VERIFICATION_MESSAGE);
    }

    public AuthIdentity verifyEmail(VerifyEmailCommand command) {
        String email = requireEmail(command.email());
        String normalizedEmail = normalizeEmail(email);
        AuthChallenge challenge = requireValidChallenge(normalizedEmail, AuthChallengeType.EMAIL_VERIFICATION,
                command.code());
        if (!challenge.subjectExists()) {
            throw new ValidationException("invalid_or_expired_code");
        }

        AuthIdentity verified = userIdentityGateway.markEmailVerified(email)
                .map(authIdentityRepository::synchronize)
                .orElseThrow(() -> new ValidationException("invalid_or_expired_code"));
        keycloakOAuthClient.synchronizeUser(verified, verified.providerSubject());
        challengeRepository.consume(challenge.id(), Instant.now());
        return verified;
    }

    public AuthChallengeAccepted requestPasswordReset(RequestPasswordResetCommand command) {
        String email = requireEmail(command.email());
        String normalizedEmail = normalizeEmail(email);
        AuthChallengeType type = AuthChallengeType.PASSWORD_RESET;
        Optional<AuthIdentity> identity = resolveIdentity(normalizedEmail);

        if (!isRateLimited(normalizedEmail, command.requestIp(), type)
                && identity.filter(this::canResetPassword).isPresent()) {
            issueChallenge(email, normalizedEmail, type, passwordResetTtlMinutes, command.requestIp(), true);
        } else if (identity.isEmpty()) {
            recordNonDeliverableChallenge(email, normalizedEmail, type, passwordResetTtlMinutes, command.requestIp());
        }

        return new AuthChallengeAccepted(GENERIC_RESET_MESSAGE);
    }

    public CodeValidationResult validatePasswordReset(ValidatePasswordResetCommand command) {
        String normalizedEmail = normalizeEmail(requireEmail(command.email()));
        AuthChallenge challenge = requireValidChallenge(normalizedEmail, AuthChallengeType.PASSWORD_RESET,
                command.code());
        if (!challenge.subjectExists()) {
            throw new ValidationException("invalid_or_expired_code");
        }
        return new CodeValidationResult(true, challenge.expiresAt());
    }

    public AuthIdentity resetPassword(ResetPasswordCommand command) {
        String email = requireEmail(command.email());
        String normalizedEmail = normalizeEmail(email);
        if (isWeakPassword(command.newPassword())) {
            throw new ValidationException("newPassword must have at least 8 characters");
        }

        AuthChallenge challenge = requireValidChallenge(normalizedEmail, AuthChallengeType.PASSWORD_RESET,
                command.code());
        if (!challenge.subjectExists()) {
            throw new ValidationException("invalid_or_expired_code");
        }

        AuthIdentity updated = userIdentityGateway.resetPassword(email, command.newPassword())
                .map(authIdentityRepository::synchronize)
                .orElseThrow(() -> new ValidationException("invalid_or_expired_code"));
        keycloakOAuthClient.createPasswordUser(updated, command.newPassword());
        challengeRepository.consume(challenge.id(), Instant.now());
        return updated;
    }

    public void sendRegistrationVerification(AuthIdentity identity) {
        if (identity == null || !needsEmailVerification(identity)) {
            return;
        }
        String normalizedEmail = normalizeEmail(identity.email());
        issueChallenge(identity.email(), normalizedEmail, AuthChallengeType.EMAIL_VERIFICATION,
                emailVerificationTtlMinutes, null, true);
    }

    private void issueChallenge(String email, String normalizedEmail, AuthChallengeType type, long ttlMinutes,
                                String requestIp, boolean subjectExists) {
        String code = generateCode();
        Instant expiresAt = Instant.now().plusSeconds(Math.max(1, ttlMinutes) * 60);
        challengeRepository.invalidateOpenChallenges(normalizedEmail, type);
        challengeRepository.create(email, normalizedEmail, type, hashCode(normalizedEmail, type, code), expiresAt,
                requestIp, subjectExists);
        if (!subjectExists) {
            return;
        }
        if (type == AuthChallengeType.EMAIL_VERIFICATION) {
            notificationGateway.sendEmailVerificationCode(email, code, expiresAt);
        } else {
            notificationGateway.sendPasswordResetCode(email, code, expiresAt);
        }
    }

    private void recordNonDeliverableChallenge(String email, String normalizedEmail, AuthChallengeType type,
                                               long ttlMinutes, String requestIp) {
        if (isRateLimited(normalizedEmail, requestIp, type)) {
            return;
        }
        issueChallenge(email, normalizedEmail, type, ttlMinutes, requestIp, false);
    }

    private AuthChallenge requireValidChallenge(String normalizedEmail, AuthChallengeType type, String code) {
        if (isBlank(code)) {
            throw new ValidationException("invalid_or_expired_code");
        }
        AuthChallenge challenge = challengeRepository.findLatestOpenChallenge(normalizedEmail, type, Instant.now())
                .orElseThrow(() -> new ValidationException("invalid_or_expired_code"));
        if (challenge.attempts() >= maxAttempts) {
            challengeRepository.consume(challenge.id(), Instant.now());
            throw new ValidationException("invalid_or_expired_code");
        }
        if (!matches(challenge.codeHash(), normalizedEmail, type, code.trim())) {
            challengeRepository.incrementAttempts(challenge.id());
            throw new ValidationException("invalid_or_expired_code");
        }
        return challenge;
    }

    private Optional<AuthIdentity> resolveIdentity(String normalizedEmail) {
        Optional<AuthIdentity> cached = authIdentityRepository.findAnyIdentityByEmail(normalizedEmail);
        if (cached.isPresent()) {
            return cached;
        }
        return userIdentityGateway.findByEmail(normalizedEmail).map(authIdentityRepository::synchronize);
    }

    private boolean needsEmailVerification(AuthIdentity identity) {
        return identity != null
                && (!identity.emailVerified() || "PENDING_EMAIL_VERIFICATION".equals(identity.status()));
    }

    private boolean canResetPassword(AuthIdentity identity) {
        return identity != null && identity.emailVerified() && "ACTIVE".equals(identity.status());
    }

    private boolean isRateLimited(String normalizedEmail, String requestIp, AuthChallengeType type) {
        Instant since = Instant.now().minusSeconds(Math.max(1, requestWindowMinutes) * 60);
        return challengeRepository.countRecentRequests(normalizedEmail, blankToNull(requestIp), type, since)
                >= maxRequestsPerWindow;
    }

    private boolean matches(String expectedHash, String normalizedEmail, AuthChallengeType type, String code) {
        byte[] expected = bytes(expectedHash);
        byte[] actual = bytes(hashCode(normalizedEmail, type, code));
        return MessageDigest.isEqual(expected, actual);
    }

    private String hashCode(String normalizedEmail, AuthChallengeType type, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = type.name() + ":" + normalizedEmail + ":" + code + ":" + codeSecret;
            return Base64.getEncoder().encodeToString(digest.digest(bytes(payload)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AuthenticationException("challenge_hash_unavailable");
        }
    }

    private String generateCode() {
        String configured = fixedCode.orElse("").trim();
        if (!configured.isBlank()) {
            return configured;
        }
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String requireEmail(String value) {
        if (isBlank(value) || !value.contains("@")) {
            throw new ValidationException("email is required");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isWeakPassword(String value) {
        return value == null || value.length() < 8;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private byte[] bytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }
}
