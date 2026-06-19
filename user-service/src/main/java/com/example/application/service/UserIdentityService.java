package com.example.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.example.application.command.GrantAccountantAccessCommand;
import com.example.application.command.RegisterTenantCommand;
import com.example.application.command.ResendEmailVerificationCodeCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.command.VerifyEmailCodeCommand;
import com.example.application.command.VerifyPasswordCommand;
import com.example.application.exception.ConflictException;
import com.example.application.exception.ForbiddenException;
import com.example.application.exception.RateLimitException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.EmailVerificationSender;
import com.example.application.port.out.InternalServiceAuthorizer;
import com.example.application.port.out.PasswordHasher;
import com.example.application.port.out.UserIdentityRepository;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.AccountantClientView;
import com.example.domain.model.EmailVerificationChallenge;
import com.example.domain.model.EmailVerificationChallengeRecord;
import com.example.domain.model.IdentityVerification;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.TenantCompanyProfile;
import com.example.domain.model.TenantContext;
import com.example.domain.model.UserView;
import com.example.infrastructure.keycloak.KeycloakAdminClient;
import com.example.infrastructure.keycloak.KeycloakIntegrationException;
import com.example.infrastructure.persistence.RepositoryException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserIdentityService {
    private static final Logger LOG = Logger.getLogger(UserIdentityService.class);
    private static final String DEFAULT_TEMPORARY_PASSWORD = "ChangeMe123!";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING_EMAIL_VERIFICATION = "PENDING_EMAIL_VERIFICATION";
    private static final int VERIFICATION_CODE_LENGTH = 6;

    @Inject
    UserIdentityRepository userIdentityRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    InternalServiceAuthorizer internalServiceAuthorizer;

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    EmailVerificationSender emailVerificationSender;

    @ConfigProperty(name = "user.email-verification.code-ttl-minutes")
    long verificationCodeTtlMinutes;

    @ConfigProperty(name = "user.email-verification.resend-cooldown-seconds")
    long resendCooldownSeconds;

    @ConfigProperty(name = "user.email-verification.max-attempts")
    int maxVerificationAttempts;

    @ConfigProperty(name = "user.email-verification.hash-secret")
    String verificationHashSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    void validateVerificationConfiguration() {
        if (verificationCodeTtlMinutes <= 0) {
            throw new IllegalStateException("user.email-verification.code-ttl-minutes must be positive");
        }
        if (resendCooldownSeconds < 0) {
            throw new IllegalStateException("user.email-verification.resend-cooldown-seconds must be >= 0");
        }
        if (maxVerificationAttempts <= 0) {
            throw new IllegalStateException("user.email-verification.max-attempts must be positive");
        }
    }

    public RegisteredTenant registerTenant(RegisterTenantCommand command) {
        if (isBlank(command.legalName()) || isBlank(command.adminName()) || isBlank(command.email())
                || isWeakPassword(command.password())) {
            throw new ValidationException("legalName, adminName, email and a password with at least 8 characters are required");
        }

        try {
            return userIdentityRepository.registerTenant(
                    command.legalName().trim(),
                    blankToNull(command.tradeName()),
                    command.adminName().trim(),
                    command.email().trim(),
                    passwordHasher.hash(command.password()),
                    new TenantCompanyProfile(
                            onlyDigitsOrNull(command.cnpj()),
                            blankToNull(command.cnaeCode()),
                            blankToNull(command.cnaeDescription()),
                            blankToNull(command.addressStreet()),
                            blankToNull(command.addressNumber()),
                            blankToNull(command.addressComplement()),
                            blankToNull(command.addressNeighborhood()),
                            blankToNull(command.addressCity()),
                            blankToNull(command.addressState()),
                            onlyDigitsOrNull(command.addressZipCode())
                    )
            );
        } catch (RepositoryException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }

    public AccountantAccessView grantAccountantAccess(GrantAccountantAccessCommand command) {
        if (isBlank(command.email()) || isBlank(command.firstName()) || isBlank(command.lastName())
                || isBlank(command.grantedByUserId())) {
            throw new ValidationException("email, firstName, lastName and grantedByUserId are required");
        }

        String email = command.email().trim();
        String firstName = command.firstName().trim();
        String lastName = command.lastName().trim();
        String fullName = command.fullName() != null ? command.fullName().trim() : firstName + " " + lastName;
        String rawPassword = command.temporaryPassword() != null ? command.temporaryPassword() : DEFAULT_TEMPORARY_PASSWORD;
        Optional<UserView> existingUser = userIdentityRepository.findUserByEmail(email);
        String accountantUserId = existingUser.map(UserView::id).orElseGet(() -> UUID.randomUUID().toString());

        Optional<String> keycloakSubject = Optional.empty();
        if (existingUser.isEmpty()) {
            try {
                keycloakSubject = keycloakAdminClient.createAccountantUser(
                        accountantUserId, command.tenantId(), email, firstName, lastName, rawPassword);
            } catch (KeycloakIntegrationException exception) {
                throw new ValidationException("Falha ao criar usuario no Keycloak: " + exception.getMessage());
            }
        }

        String provider = existingUser.map(UserView::provider).orElse(keycloakSubject.isPresent() ? "KEYCLOAK" : "PASSWORD");
        String providerSubject = existingUser.map(UserView::providerSubject).orElse(keycloakSubject.orElse(null));
        String status = existingUser.map(UserView::status).orElse(keycloakSubject.isPresent() ? STATUS_ACTIVE : "INVITED");

        String passwordHash = keycloakSubject.isPresent()
                ? "KEYCLOAK_MANAGED_" + UUID.randomUUID()
                : passwordHasher.hash(rawPassword);

        try {
            return userIdentityRepository.grantAccountantAccess(
                    accountantUserId,
                    command.tenantId(),
                    email,
                    fullName,
                    firstName,
                    lastName,
                    passwordHash,
                    provider,
                    providerSubject,
                    status,
                    command.grantedByUserId()
            );
        } catch (RepositoryException exception) {
            throw new ValidationException(exception.getMessage());
        }
    }

    public List<UserView> listTenantMembers(String tenantId) {
        return userIdentityRepository.listTenantUsers(tenantId);
    }

    public List<AccountantClientView> listAccountantClients(TenantContext context) {
        if (isGlobalBpoOperator(context)) {
            return userIdentityRepository.listAllBpoClients();
        }
        if (!context.roles().contains("CONTADOR")) {
            throw new ForbiddenException("accountant_role_required");
        }
        return userIdentityRepository.listAccountantClients(context.userId(), context.email());
    }

    public Optional<IdentityVerification> verifyPassword(String internalToken, VerifyPasswordCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new ValidationException("email and password are required");
        }

        return userIdentityRepository.findCredentialsByEmail(command.email())
                .filter(credentials -> passwordHasher.verify(command.password(), credentials.passwordHash()))
                .map(this::toVerification);
    }

    public EmailVerificationChallenge resendEmailVerificationCode(String internalToken,
                                                                  ResendEmailVerificationCodeCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email())) {
            throw new ValidationException("email is required");
        }

        UserView user = userIdentityRepository.findUserByEmail(command.email().trim())
                .orElseThrow(() -> new ValidationException("user_not_found"));

        if (user.emailVerified() || STATUS_ACTIVE.equals(user.status())) {
            throw new ValidationException("email_already_verified");
        }

        return issueEmailVerificationCode(user, false);
    }

    public UserView verifyEmailCode(String internalToken, VerifyEmailCodeCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email()) || isBlank(command.code())) {
            throw new ValidationException("email and code are required");
        }

        UserView user = userIdentityRepository.findUserByEmail(command.email().trim())
                .orElseThrow(() -> new ValidationException("user_not_found"));

        if (user.emailVerified() || STATUS_ACTIVE.equals(user.status())) {
            return user;
        }

        EmailVerificationChallengeRecord challenge = userIdentityRepository.findEmailVerificationChallenge(command.email().trim())
                .orElseThrow(() -> new ValidationException("verification_code_expired"));

        Instant now = Instant.now();
        if (challenge.expiresAt().isBefore(now)) {
            userIdentityRepository.deleteEmailVerificationChallenge(command.email().trim());
            throw new ValidationException("verification_code_expired");
        }

        String expectedHash = hashVerificationCode(command.email().trim(), command.code().trim(), challenge.codeSalt());
        if (!constantTimeEquals(expectedHash, challenge.codeHash())) {
            int remainingAttempts = Math.max(0, challenge.attemptsRemaining() - 1);
            if (remainingAttempts == 0) {
                userIdentityRepository.deleteEmailVerificationChallenge(command.email().trim());
            } else {
                userIdentityRepository.updateEmailVerificationAttempts(command.email().trim(), remainingAttempts, now);
            }
            throw new ValidationException("invalid_verification_code");
        }

        UserView verifiedUser = userIdentityRepository.activateUserEmail(command.email().trim(), now)
                .orElseThrow(() -> new ValidationException("user_not_found"));
        userIdentityRepository.deleteEmailVerificationChallenge(command.email().trim());
        return verifiedUser;
    }

    public Optional<UserView> syncExternalProfile(String internalToken, SyncExternalProfileCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email()) || isBlank(command.provider()) || isBlank(command.providerSubject())) {
            throw new ValidationException("email, provider and providerSubject are required");
        }

        return userIdentityRepository.syncExternalProfile(
                command.email().trim(),
                command.provider().trim().toUpperCase(Locale.ROOT),
                command.providerSubject().trim(),
                blankToNull(command.fullName()),
                blankToNull(command.preferredUsername()),
                blankToNull(command.firstName()),
                blankToNull(command.lastName()),
                blankToNull(command.pictureUrl()),
                command.emailVerified()
        );
    }

    private IdentityVerification toVerification(StoredUserCredentials credentials) {
        return new IdentityVerification(
                credentials.userId(),
                credentials.tenantId(),
                credentials.email(),
                credentials.fullName(),
                credentials.preferredUsername(),
                credentials.firstName(),
                credentials.lastName(),
                credentials.pictureUrl(),
                credentials.emailVerified(),
                credentials.provider(),
                credentials.providerSubject(),
                credentials.status(),
                credentials.roles(),
                userIdentityRepository.listAccountantTenantIds(credentials.userId(), credentials.email())
        );
    }

    private EmailVerificationChallenge issueEmailVerificationCode(UserView user, boolean ignoreCooldown) {
        Instant now = Instant.now();
        Optional<EmailVerificationChallengeRecord> existingChallenge = userIdentityRepository
                .findEmailVerificationChallenge(user.email());

        if (!ignoreCooldown && existingChallenge.isPresent()
                && existingChallenge.get().lastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new RateLimitException("verification_code_recently_sent");
        }

        String code = generateVerificationCode();
        String salt = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(verificationCodeTtlMinutes, ChronoUnit.MINUTES);

        userIdentityRepository.saveEmailVerificationChallenge(
                user.email(),
                hashVerificationCode(user.email(), code, salt),
                salt,
                expiresAt,
                maxVerificationAttempts,
                now
        );

        try {
            emailVerificationSender.send(user.tenantId(), user.email(), user.fullName(), code, expiresAt);
        } catch (RuntimeException exception) {
            LOG.warnf(exception, "Failed to dispatch email verification code for %s", user.email());
        }

        return new EmailVerificationChallenge(user.email(), expiresAt, now);
    }

    private String generateVerificationCode() {
        int value = secureRandom.nextInt((int) Math.pow(10, VERIFICATION_CODE_LENGTH));
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", value);
    }

    private String hashVerificationCode(String email, String code, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((normalizeEmail(email) + ":" + code + ":" + salt + ":" + verificationHashSecret)
                    .getBytes(StandardCharsets.UTF_8));
            return toHex(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash verification code", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isWeakPassword(String value) {
        return value == null || value.length() < 8;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String onlyDigitsOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isGlobalBpoOperator(TenantContext context) {
        return context.roles().contains("BPO_ADMIN")
                || (context.roles().contains("ADMIN") && context.roles().contains("CONTADOR"));
    }
}
